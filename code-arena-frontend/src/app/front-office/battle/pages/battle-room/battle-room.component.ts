import { Component, OnInit, OnDestroy, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BattleWebsocketService } from '../../services/battle-websocket.service';
import { BattleService } from '../../services/battle.service';
import {
  ArenaStateResponse,
  ArenaChallengeResponse,
  OpponentProgressEvent,
  MatchFinishedEvent,
  SubmissionResultResponse,
  ProgressPulse,
} from '../../models/battle-room.model';

@Component({
  selector: 'app-battle-room',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './battle-room.component.html',
  styleUrls: ['./battle-room.component.css'],
})
export class BattleRoomComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ws = inject(BattleWebsocketService);
  private readonly battleService = inject(BattleService);
  private readonly destroy$ = new Subject<void>();

  roomId = '';
  arenaState: ArenaStateResponse | null = null;
  lastSubmissionResult: SubmissionResultResponse | null = null;

  activeChallengeIndex = 0;
  selectedLanguage = 'javascript';
  code = '';
  submitting = false;
  lineCount = 20;
  leftPanelWidth = 40;
  private resizing = false;

  opponentPulses: Record<string, ProgressPulse | null> = {};
  private solvedChallenges = new Set<string>();

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('roomId') ?? '';
    if (!this.roomId) return;

    this.ws.connect(this.roomId);

    this.battleService.getArenaState(this.roomId).subscribe({
      next: (state) => (this.arenaState = state),
      error: () => {},
    });

    this.ws.arenaState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => (this.arenaState = e.payload));

    this.ws.opponentProgress$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onOpponentProgress(e.payload));

    this.ws.matchFinished$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onMatchFinished(e.payload));

    this.ws.matchCancelled$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.router.navigate(['/battle']));

    this.ws.submissionResult$
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => this.onSubmissionResult(result));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
  }

  get activeChallenge(): ArenaChallengeResponse | null {
    return this.arenaState?.challenges[this.activeChallengeIndex] ?? null;
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  isChallengeSolved(roomChallengeId: string): boolean {
    return this.solvedChallenges.has(roomChallengeId);
  }

  getLineNumbers(): number[] {
    return Array.from({ length: this.lineCount }, (_, i) => i + 1);
  }

  onCodeInput(): void {
    const lines = this.code.split('\n').length;
    this.lineCount = Math.max(20, lines + 5);
  }

  onEditorKeydown(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      event.preventDefault();
      const textarea = event.target as HTMLTextAreaElement;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      this.code = this.code.substring(0, start) + '  ' + this.code.substring(end);
      setTimeout(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 2;
      });
    }
  }

  submitCode(): void {
    if (!this.activeChallenge || this.submitting) return;
    this.submitting = true;
    this.lastSubmissionResult = null;

    this.battleService.submitSolution(this.roomId, {
      roomId: this.roomId,
      roomChallengeId: this.activeChallenge.roomChallengeId,
      language: this.selectedLanguage,
      code: this.code,
    }).subscribe({
      next: () => {
        // HTTP response only confirms receipt (PENDING).
        // The actual judging result arrives via WebSocket (submissionResult$).
      },
      error: () => {
        this.submitting = false;
      },
    });
  }

  private onSubmissionResult(result: SubmissionResultResponse): void {
    this.lastSubmissionResult = result;
    this.submitting = false;

    if (result.isAccepted) {
      this.solvedChallenges.add(result.roomChallengeId);
      // Auto-advance to next unsolved challenge
      if (this.arenaState) {
        const nextIndex = this.arenaState.challenges.findIndex(
          (c, i) => i > this.activeChallengeIndex && !this.solvedChallenges.has(c.roomChallengeId)
        );
        if (nextIndex >= 0) {
          setTimeout(() => (this.activeChallengeIndex = nextIndex), 1000);
        }
      }
    }
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

    // Show pulse animation
    this.opponentPulses[progress.participantId] = progress.pulse;
    setTimeout(() => {
      this.opponentPulses[progress.participantId] = null;
    }, 800);
  }

  private onMatchFinished(_event: MatchFinishedEvent): void {
    this.router.navigate(['/battle/result', this.roomId]);
  }

  // ── Panel Resizing ─────────────────────────────────────────

  startResize(event: MouseEvent): void {
    this.resizing = true;
    event.preventDefault();
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    if (!this.resizing) return;
    const percentage = (event.clientX / window.innerWidth) * 100;
    this.leftPanelWidth = Math.max(25, Math.min(70, percentage));
  }

  @HostListener('document:mouseup')
  onMouseUp(): void {
    this.resizing = false;
  }
}
