import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '@auth0/auth0-angular';
import { BattleWebsocketService } from '../../services/battle-websocket.service';
import { BattleService } from '../../services/battle.service';
import {
  LobbyStateResponse,
  ParticipantResponse,
  BattleRoomResponse,
  CountdownPayload,
} from '../../models/battle-room.model';

interface LogEntry {
  type: 'join' | 'leave' | 'ready' | 'info';
  message: string;
  time: string;
}

@Component({
  selector: 'app-battle-lobby',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-lobby.component.html',
  styleUrls: ['./battle-lobby.component.css'],
})
export class BattleLobbyComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ws = inject(BattleWebsocketService);
  private readonly battleService = inject(BattleService);
  private readonly auth = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  roomId = '';
  lobbyState: LobbyStateResponse | null = null;
  currentUserId = '';
  myReadyState = false;
  readyUpdating = false;
  isHost = false;
  inviteCopied = false;
  startingBattle = false;
  private battleStartFallbackTimer: ReturnType<typeof setTimeout> | null = null;
  eventLog: LogEntry[] = [];

  countdownActive = false;
  countdownValue = 0;
  countdownTotal = 0;
  countdownDashOffset = 0;

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('roomId') ?? '';
    if (!this.roomId) {
      this.router.navigate(['/battle']);
      return;
    }

    this.auth.user$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      if (user?.sub) {
        this.currentUserId = user.sub;
        if (this.lobbyState) {
          this.applyLobbyState(this.lobbyState);
        }
      }
    });

    this.ws.connect(this.roomId);

    this.battleService.getLobbyState(this.roomId).subscribe({
      next: (state) => this.applyLobbyState(state),
      error: () => {},
    });

    this.ws.lobbyState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.applyLobbyState(e.payload));

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
    if (this.battleStartFallbackTimer) {
      clearTimeout(this.battleStartFallbackTimer);
      this.battleStartFallbackTimer = null;
    }
    this.ws.disconnect();
  }

  private applyLobbyState(state: LobbyStateResponse): void {
    this.lobbyState = state;
    this.isHost = state.room.hostId === this.currentUserId;
    const me = state.players.find(p => p.userId === this.currentUserId);
    if (me) this.myReadyState = me.isReady;
  }

  // ── WebSocket event handlers ──────────────────────────────

  private onPlayerJoined(player: ParticipantResponse): void {
    if (this.lobbyState) {
      this.lobbyState = {
        ...this.lobbyState,
        players: [...this.lobbyState.players, player],
      };
      this.addLog('join', `${player.username} joined the arena`);
    }
  }

  private onPlayerLeft(userId: string): void {
    if (this.lobbyState) {
      const player = this.lobbyState.players.find(p => p.userId === userId);
      this.lobbyState = {
        ...this.lobbyState,
        players: this.lobbyState.players.filter((p) => p.userId !== userId),
      };
      if (player) this.addLog('leave', `${player.username} left the arena`);
    }
  }

  private onPlayerKicked(userId: string): void {
    if (userId === this.currentUserId) {
      this.router.navigate(['/battle']);
      return;
    }
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
      if (player.userId === this.currentUserId) {
        this.myReadyState = player.isReady;
      }
      this.addLog('ready', `${player.username} is ${player.isReady ? 'ready' : 'not ready'}`);
    }
  }

  private onCountdownStarted(countdown: CountdownPayload): void {
    this.countdownActive = true;
    this.countdownTotal = countdown.countdownSeconds;
    this.countdownValue = countdown.countdownSeconds;

    const circumference = 2 * Math.PI * 54; // r=54

    interval(1000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.countdownValue--;
      this.countdownDashOffset = circumference * (1 - this.countdownValue / this.countdownTotal);
      if (this.countdownValue <= 0) {
        this.countdownActive = false;
      }
    });
  }

  private onBattleStarted(_room: BattleRoomResponse): void {
    if (this.battleStartFallbackTimer) {
      clearTimeout(this.battleStartFallbackTimer);
      this.battleStartFallbackTimer = null;
    }
    this.router.navigate(['/battle/room', this.roomId]);
  }

  // ── Actions ───────────────────────────────────────────────

  toggleReady(): void {
    if (this.readyUpdating) {
      return;
    }

    const nextReady = !this.myReadyState;
    this.readyUpdating = true;
    this.battleService.toggleReady(this.roomId, nextReady).subscribe({
      next: (state) => {
        this.applyLobbyState(state);
        this.readyUpdating = false;
      },
      error: () => {
        this.readyUpdating = false;
      }
    });
  }

  startBattle(): void {
    if (this.startingBattle) {
      return;
    }

    this.startingBattle = true;
    this.battleService.startBattle(this.roomId).subscribe({
      next: () => {
        // The host should be moved by the BATTLE_STARTED websocket event.
        // If the websocket handshake is flaky, fall back to navigation after the countdown window.
        if (this.battleStartFallbackTimer) {
          clearTimeout(this.battleStartFallbackTimer);
        }
        this.battleStartFallbackTimer = setTimeout(() => {
          this.router.navigate(['/battle/room', this.roomId]);
        }, 7000);
      },
      error: (error) => {
        const message = error?.error?.message ?? error?.message ?? '';
        if (error?.status === 409) {
          this.router.navigate(['/battle/room', this.roomId]);
          return;
        }

        this.startingBattle = false;
      },
    });
  }

  kickPlayer(userId: string): void {
    this.battleService.kickParticipant(this.roomId, { targetUserId: userId }).subscribe();
  }

  leaveLobby(): void {
    this.battleService.leaveRoom(this.roomId).subscribe({
      next: () => this.router.navigate(['/battle']),
      error: () => this.router.navigate(['/battle']),
    });
  }

  copyInviteLink(): void {
    const token = this.lobbyState?.room.inviteToken;
    if (token) {
      navigator.clipboard.writeText(token);
      this.inviteCopied = true;
      setTimeout(() => (this.inviteCopied = false), 2000);
    }
  }

  getDuelSlots(): (ParticipantResponse | null)[] {
    const slots: (ParticipantResponse | null)[] = [null, null];
    if (this.lobbyState) {
      this.lobbyState.players.forEach((p, i) => {
        if (i < 2) slots[i] = p;
      });
    }
    return slots;
  }

  getMultiSlots(): (ParticipantResponse | null)[] {
    if (!this.lobbyState) return [];
    const slots: (ParticipantResponse | null)[] = [...this.lobbyState.players];
    const empty = this.lobbyState.room.maxPlayers - slots.length;
    for (let i = 0; i < empty; i++) slots.push(null);
    return slots;
  }

  private addLog(type: LogEntry['type'], message: string): void {
    const now = new Date();
    const time = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.eventLog.unshift({ type, message, time });
    if (this.eventLog.length > 20) this.eventLog.pop();
  }
}
