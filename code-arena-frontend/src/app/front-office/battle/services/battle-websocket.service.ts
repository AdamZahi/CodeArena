import { Injectable, OnDestroy, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject, firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  LobbyEvent,
  LobbyStateResponse,
  ParticipantResponse,
  BattleRoomResponse,
  CountdownPayload,
  ArenaStateResponse,
  OpponentProgressEvent,
  MatchFinishedEvent,
  SpectatorFeedEvent,
  SubmissionResultResponse,
} from '../models/battle-room.model';

@Injectable({ providedIn: 'root' })
export class BattleWebsocketService implements OnDestroy {

  private readonly auth = inject(AuthService);
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];

  // ── Lobby observables ────────────────────────────────────
  readonly lobbyState$    = new Subject<LobbyEvent<LobbyStateResponse>>();
  readonly playerJoined$  = new Subject<LobbyEvent<ParticipantResponse>>();
  readonly playerLeft$    = new Subject<LobbyEvent<string>>();
  readonly playerKicked$  = new Subject<LobbyEvent<string>>();
  readonly readyChanged$  = new Subject<LobbyEvent<ParticipantResponse>>();
  readonly countdownStarted$ = new Subject<LobbyEvent<CountdownPayload>>();
  readonly battleStarted$ = new Subject<LobbyEvent<BattleRoomResponse>>();
  readonly roomCancelled$ = new Subject<LobbyEvent<string>>();

  // ── Arena observables ────────────────────────────────────
  readonly arenaState$        = new Subject<LobbyEvent<ArenaStateResponse>>();
  readonly opponentProgress$  = new Subject<LobbyEvent<OpponentProgressEvent>>();
  readonly matchFinished$     = new Subject<LobbyEvent<MatchFinishedEvent>>();
  readonly matchCancelled$    = new Subject<LobbyEvent<string>>();

  // ── Spectator observable ─────────────────────────────────
  readonly spectatorFeed$ = new Subject<LobbyEvent<SpectatorFeedEvent>>();

  // ── User-specific submission result ──────────────────────
  readonly submissionResult$ = new Subject<SubmissionResultResponse>();

  // ── Connection ───────────────────────────────────────────

  async connect(roomId: string): Promise<void> {
    if (this.client?.active) return;

    const token = await firstValueFrom(
      this.auth.getAccessTokenSilently()
    );

    const wsProtocol = environment.apiBaseUrl.startsWith('https') ? 'wss' : 'ws';
    const host = environment.apiBaseUrl.replace(/^https?:\/\//, '');

    this.client = new Client({
      brokerURL: `${wsProtocol}://${host}/ws/websocket`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,

      onConnect: () => {
        console.log('[BattleWS] Connected');
        this.subscribeLobby(roomId);
        this.subscribeArena(roomId);
        this.subscribeSpectator(roomId);
        this.subscribeSubmission();
      },

      onDisconnect: () => console.log('[BattleWS] Disconnected'),
      onStompError: (frame) => console.error('[BattleWS] STOMP error', frame),
    });

    this.client.activate();
  }

  disconnect(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
    this.subscriptions = [];
    this.client?.deactivate();
    this.client = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  // ── Topic subscriptions ──────────────────────────────────

  private subscribeLobby(roomId: string): void {
    const sub = this.client!.subscribe(
      `/topic/battle/lobby/${roomId}`,
      (msg: IMessage) => {
        const event: LobbyEvent = JSON.parse(msg.body);
        switch (event.type) {
          case 'LOBBY_STATE':       this.lobbyState$.next(event as LobbyEvent<LobbyStateResponse>); break;
          case 'PLAYER_JOINED':     this.playerJoined$.next(event as LobbyEvent<ParticipantResponse>); break;
          case 'PLAYER_LEFT':       this.playerLeft$.next(event as LobbyEvent<string>); break;
          case 'PLAYER_KICKED':     this.playerKicked$.next(event as LobbyEvent<string>); break;
          case 'READY_CHANGED':     this.readyChanged$.next(event as LobbyEvent<ParticipantResponse>); break;
          case 'COUNTDOWN_STARTED': this.countdownStarted$.next(event as LobbyEvent<CountdownPayload>); break;
          case 'BATTLE_STARTED':    this.battleStarted$.next(event as LobbyEvent<BattleRoomResponse>); break;
          case 'ROOM_CANCELLED':    this.roomCancelled$.next(event as LobbyEvent<string>); break;
        }
      }
    );
    this.subscriptions.push(sub);
  }

  private subscribeArena(roomId: string): void {
    const sub = this.client!.subscribe(
      `/topic/battle/arena/${roomId}`,
      (msg: IMessage) => {
        const event: LobbyEvent = JSON.parse(msg.body);
        switch (event.type) {
          case 'ARENA_STATE':       this.arenaState$.next(event as LobbyEvent<ArenaStateResponse>); break;
          case 'OPPONENT_PROGRESS': this.opponentProgress$.next(event as LobbyEvent<OpponentProgressEvent>); break;
          case 'MATCH_FINISHED':    this.matchFinished$.next(event as LobbyEvent<MatchFinishedEvent>); break;
          case 'MATCH_CANCELLED':   this.matchCancelled$.next(event as LobbyEvent<string>); break;
        }
      }
    );
    this.subscriptions.push(sub);
  }

  private subscribeSpectator(roomId: string): void {
    const sub = this.client!.subscribe(
      `/topic/battle/spectator/${roomId}`,
      (msg: IMessage) => {
        const event: LobbyEvent = JSON.parse(msg.body);
        if (event.type === 'SPECTATOR_FEED') {
          this.spectatorFeed$.next(event as LobbyEvent<SpectatorFeedEvent>);
        }
      }
    );
    this.subscriptions.push(sub);
  }

  private subscribeSubmission(): void {
    const sub = this.client!.subscribe(
      '/user/queue/battle/submission',
      (msg: IMessage) => {
        const event: LobbyEvent<SubmissionResultResponse> = JSON.parse(msg.body);
        this.submissionResult$.next(event.payload);
      }
    );
    this.subscriptions.push(sub);
  }
}
