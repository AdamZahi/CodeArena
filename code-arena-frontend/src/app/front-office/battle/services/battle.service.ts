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
  ActivityRequest,
  ShareUrlResponse,
  SharedResultDTO,
  MatchComparisonResponse,
} from '../models/battle-room.model';
import { MatchHistoryResponse } from '../../reward-profile/models/match-history.model';
import { SeasonLeaderboardResponse, XpLeaderboardResponse } from '../../reward-profile/models/leaderboard.model';

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

  // ── Activity & Connection endpoints ────────────────────────

  reportActivity(roomId: string, request: ActivityRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/arena/${roomId}/activity`, request);
  }

  reconnect(roomId: string): Observable<ArenaStateResponse> {
    return this.http.post<ArenaStateResponse>(`${this.baseUrl}/arena/${roomId}/reconnect`, {});
  }

  sendHeartbeat(roomId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/arena/${roomId}/heartbeat`, {});
  }

  // ── Results endpoints (/api/battle/results) ───────────────

  getScoreboard(roomId: string): Observable<PostMatchSummaryResponse> {
    return this.http.get<PostMatchSummaryResponse>(`${this.baseUrl}/results/${roomId}/scoreboard`);
  }

  getMatchComparison(roomId: string): Observable<MatchComparisonResponse> {
    return this.http.get<MatchComparisonResponse>(`${this.baseUrl}/results/${roomId}/compare`);
  }

  // ── Shareable Result endpoints ────────────────────────────

  createShareToken(roomId: string): Observable<ShareUrlResponse> {
    return this.http.post<ShareUrlResponse>(`${this.baseUrl}/results/${roomId}/share`, {});
  }

  getSharedResult(token: string): Observable<SharedResultDTO> {
    return this.http.get<SharedResultDTO>(`${this.baseUrl}/results/share/${token}`);
  }

  // ── History & Leaderboard endpoints ─────────────────────────

  getMyMatchHistory(page: number = 0, size: number = 10): Observable<MatchHistoryResponse> {
    return this.http.get<MatchHistoryResponse>(`${this.baseUrl}/history/me?page=${page}&size=${size}`);
  }

  getUserMatchHistory(userId: string, page: number = 0, size: number = 10): Observable<MatchHistoryResponse> {
    return this.http.get<MatchHistoryResponse>(`${this.baseUrl}/history/${userId}?page=${page}&size=${size}`);
  }

  getSeasonLeaderboard(page: number = 0, size: number = 50): Observable<SeasonLeaderboardResponse> {
    return this.http.get<SeasonLeaderboardResponse>(`${this.baseUrl}/leaderboard/season?page=${page}&size=${size}`);
  }

  getXpLeaderboard(page: number = 0, size: number = 50): Observable<XpLeaderboardResponse> {
    return this.http.get<XpLeaderboardResponse>(`${this.baseUrl}/leaderboard/xp?page=${page}&size=${size}`);
  }

  getBattleProfile(userId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/profile/${userId}`);
  }
}
