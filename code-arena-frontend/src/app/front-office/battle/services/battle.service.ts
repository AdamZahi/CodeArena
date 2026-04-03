import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  BattleRoomResponse,
  CreateRoomRequest,
  RoomCreatedResponse,
  LobbyStateResponse,
  JoinRoomRequest,
  ReadyToggleRequest,
  KickParticipantRequest,
  InviteLinkResponse,
  ArenaStateResponse,
  ArenaChallengeResponse,
  SubmitSolutionRequest,
  SubmissionResultResponse,
  PostMatchSummaryResponse,
} from '../models/battle-room.model';

@Injectable({ providedIn: 'root' })
export class BattleService extends ApiService {
  constructor(http: HttpClient) {
    super(http, '/api/battle');
  }

  // ── Room endpoints (/api/battle/rooms) ────────────────────

  getPublicRooms(): Observable<BattleRoomResponse[]> {
    return this.http.get<BattleRoomResponse[]>(`${this.baseUrl}/rooms/public`);
  }

  createRoom(request: CreateRoomRequest): Observable<RoomCreatedResponse> {
    return this.http.post<RoomCreatedResponse>(`${this.baseUrl}/rooms`, request);
  }

  joinRoom(request: JoinRoomRequest): Observable<LobbyStateResponse> {
    return this.http.post<LobbyStateResponse>(`${this.baseUrl}/rooms/join`, request);
  }

  getLobbyState(roomId: string): Observable<LobbyStateResponse> {
    return this.http.get<LobbyStateResponse>(`${this.baseUrl}/rooms/${roomId}/lobby`);
  }

  toggleReady(roomId: string, ready: boolean): Observable<LobbyStateResponse> {
    return this.http.post<LobbyStateResponse>(`${this.baseUrl}/rooms/${roomId}/ready`, { ready } as ReadyToggleRequest);
  }

  startBattle(roomId: string): Observable<LobbyStateResponse> {
    return this.http.post<LobbyStateResponse>(`${this.baseUrl}/rooms/${roomId}/start`, {});
  }

  kickParticipant(roomId: string, request: KickParticipantRequest): Observable<LobbyStateResponse> {
    return this.http.post<LobbyStateResponse>(`${this.baseUrl}/rooms/${roomId}/kick`, request);
  }

  leaveRoom(roomId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rooms/${roomId}/leave`);
  }

  getInviteLink(roomId: string): Observable<InviteLinkResponse> {
    return this.http.get<InviteLinkResponse>(`${this.baseUrl}/rooms/${roomId}/invite`);
  }

  // ── Arena endpoints (/api/battle/arena) ───────────────────

  getArenaState(roomId: string): Observable<ArenaStateResponse> {
    return this.http.get<ArenaStateResponse>(`${this.baseUrl}/arena/${roomId}/state`);
  }

  submitSolution(roomId: string, request: SubmitSolutionRequest): Observable<SubmissionResultResponse> {
    return this.http.post<SubmissionResultResponse>(`${this.baseUrl}/arena/${roomId}/submit`, request);
  }

  getParticipantSubmissions(roomId: string, participantId: string): Observable<SubmissionResultResponse[]> {
    return this.http.get<SubmissionResultResponse[]>(`${this.baseUrl}/arena/${roomId}/participants/${participantId}/submissions`);
  }

  getRoomChallenges(roomId: string): Observable<ArenaChallengeResponse[]> {
    return this.http.get<ArenaChallengeResponse[]>(`${this.baseUrl}/arena/${roomId}/challenges`);
  }

  // ── Results endpoints (/api/battle/results) ───────────────

  getScoreboard(roomId: string): Observable<PostMatchSummaryResponse> {
    return this.http.get<PostMatchSummaryResponse>(`${this.baseUrl}/results/${roomId}/scoreboard`);
  }
}
