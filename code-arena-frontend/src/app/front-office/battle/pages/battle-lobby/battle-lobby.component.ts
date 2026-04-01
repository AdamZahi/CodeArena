import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BattleWebsocketService } from '../../services/battle-websocket.service';
import {
  LobbyStateResponse,
  ParticipantResponse,
  BattleRoomResponse,
  CountdownPayload,
} from '../../models/battle-room.model';

@Component({
  selector: 'app-battle-lobby',
  standalone: true,
  template: '<p>battle-lobby works</p>'
})
export class BattleLobbyComponent implements OnInit, OnDestroy {

  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ws     = inject(BattleWebsocketService);
  private readonly destroy$ = new Subject<void>();

  roomId = '';
  lobbyState: LobbyStateResponse | null = null;

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('roomId') ?? '';
    if (!this.roomId) return;

    this.ws.connect(this.roomId);

    this.ws.lobbyState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => (this.lobbyState = e.payload));

    this.ws.playerJoined$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onPlayerJoined(e.payload));

    this.ws.playerLeft$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onPlayerLeft(e.payload));

    this.ws.playerKicked$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onPlayerKicked(e.payload));

    this.ws.readyChanged$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onReadyChanged(e.payload));

    this.ws.countdownStarted$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onCountdownStarted(e.payload));

    this.ws.battleStarted$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.onBattleStarted(e.payload));

    this.ws.roomCancelled$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.router.navigate(['/battle']));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
  }

  // ── Event handlers (fill in with UI logic) ───────────────

  private onPlayerJoined(player: ParticipantResponse): void {
    if (this.lobbyState) {
      this.lobbyState = {
        ...this.lobbyState,
        players: [...this.lobbyState.players, player],
      };
    }
  }

  private onPlayerLeft(userId: string): void {
    if (this.lobbyState) {
      this.lobbyState = {
        ...this.lobbyState,
        players: this.lobbyState.players.filter((p) => p.userId !== userId),
      };
    }
  }

  private onPlayerKicked(userId: string): void {
    this.onPlayerLeft(userId);
  }

  private onReadyChanged(player: ParticipantResponse): void {
    if (this.lobbyState) {
      this.lobbyState = {
        ...this.lobbyState,
        players: this.lobbyState.players.map((p) =>
          p.participantId === player.participantId ? player : p
        ),
      };
    }
  }

  private onCountdownStarted(_countdown: CountdownPayload): void {
    // TODO: show countdown timer in UI
  }

  private onBattleStarted(_room: BattleRoomResponse): void {
    this.router.navigate(['/battle/room', this.roomId]);
  }
}
