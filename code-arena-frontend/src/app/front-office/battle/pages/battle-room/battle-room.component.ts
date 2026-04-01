import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BattleWebsocketService } from '../../services/battle-websocket.service';
import {
  ArenaStateResponse,
  OpponentProgressEvent,
  MatchFinishedEvent,
  SubmissionResultResponse,
} from '../../models/battle-room.model';

@Component({
  selector: 'app-battle-room',
  standalone: true,
  template: '<p>battle-room works</p>'
})
export class BattleRoomComponent implements OnInit, OnDestroy {

  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ws     = inject(BattleWebsocketService);
  private readonly destroy$ = new Subject<void>();

  roomId = '';
  arenaState: ArenaStateResponse | null = null;
  lastSubmissionResult: SubmissionResultResponse | null = null;

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('roomId') ?? '';
    if (!this.roomId) return;

    this.ws.connect(this.roomId);

    // ── Arena state (full snapshot) ────────────────────────
    this.ws.arenaState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => (this.arenaState = e.payload));

    // ── Opponent progress (incremental) ────────────────────
    this.ws.opponentProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onOpponentProgress(e.payload));

    // ── Match finished → navigate to result page ───────────
    this.ws.matchFinished$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onMatchFinished(e.payload));

    // ── Match cancelled → back to lobby list ───────────────
    this.ws.matchCancelled$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.router.navigate(['/battle']));

    // ── Personal submission result (user-destination) ──────
    this.ws.submissionResult$
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => (this.lastSubmissionResult = result));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
  }

  private onOpponentProgress(progress: OpponentProgressEvent): void {
    if (!this.arenaState) return;
    this.arenaState = {
      ...this.arenaState,
      participants: this.arenaState.participants.map((p) =>
        p.participantId === progress.participantId
          ? {
              ...p,
              challengesCompleted: progress.challengesCompleted,
              currentChallengePosition: progress.currentChallengePosition,
              totalAttempts: progress.totalAttempts,
              isFinished: progress.isFinished,
            }
          : p
      ),
    };
  }

  private onMatchFinished(_event: MatchFinishedEvent): void {
    this.router.navigate(['/battle/result', this.roomId]);
  }
}
