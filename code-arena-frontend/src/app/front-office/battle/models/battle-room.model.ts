// ── Enums ────────────────────────────────────────────────────

export type BattleRoomStatus = 'WAITING' | 'COUNTDOWN' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED';

export type LobbyEventType =
  | 'LOBBY_STATE'
  | 'PLAYER_JOINED'
  | 'PLAYER_LEFT'
  | 'PLAYER_KICKED'
  | 'READY_CHANGED'
  | 'COUNTDOWN_STARTED'
  | 'BATTLE_STARTED'
  | 'ROOM_CANCELLED'
  | 'ARENA_STATE'
  | 'OPPONENT_PROGRESS'
  | 'MATCH_FINISHED'
  | 'MATCH_CANCELLED'
  | 'SPECTATOR_FEED'
  | 'SUBMISSION_RESULT';

export type BattleSubmissionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT'
  | 'RUNTIME_ERROR'
  | 'COMPILE_ERROR';

export type ProgressPulse = 'ACCEPTED' | 'FAILED';

// ── WebSocket envelope ───────────────────────────────────────

export interface LobbyEvent<T = unknown> {
  type: LobbyEventType;
  roomId: string;
  payload: T;
}

// ── Shared DTOs ──────────────────────────────────────────────

export interface ParticipantResponse {
  participantId: string;
  userId: string;
  username: string;
  avatarUrl: string;
  role: string;
  isReady: boolean;
  currentElo: number;
  tier: string;
}

export interface BattleRoomResponse {
  id: string;
  mode: string;
  status: string;
  maxPlayers: number;
  challengeCount: number;
  isPublic: boolean;
  inviteToken: string;
  hostId: string;
  createdAt: string;
  startsAt: string;
  participants: ParticipantResponse[];
  spectatorCount: number;
}

// ── Lobby payloads ───────────────────────────────────────────

export interface LobbyStateResponse {
  room: BattleRoomResponse;
  players: ParticipantResponse[];
  spectators: ParticipantResponse[];
  canStart: boolean;
  countdownSeconds: number;
}

export interface CountdownPayload {
  countdownSeconds: number;
  battleStartsAt: string;
}

// ── Arena payloads ───────────────────────────────────────────

export interface VisibleTestCaseResponse {
  input: string;
  expectedOutput: string;
}

export interface ArenaChallengeResponse {
  roomChallengeId: string;
  position: number;
  challengeId: string;
  title: string;
  description: string;
  difficulty: string;
  tags: string;
  visibleTestCases: VisibleTestCaseResponse[];
}

export interface ArenaParticipantProgressResponse {
  participantId: string;
  userId: string;
  username: string;
  avatarUrl: string;
  challengesCompleted: number;
  currentChallengePosition: number;
  totalAttempts: number;
  isFinished: boolean;
}

export interface ArenaStateResponse {
  roomId: string;
  status: BattleRoomStatus;
  challenges: ArenaChallengeResponse[];
  participants: ArenaParticipantProgressResponse[];
  remainingSeconds: number;
}

export interface OpponentProgressEvent {
  participantId: string;
  userId: string;
  challengesCompleted: number;
  currentChallengePosition: number;
  totalAttempts: number;
  isFinished: boolean;
  pulse: ProgressPulse;
}

export interface MatchFinishedEvent {
  roomId: string;
  triggerReason: string;
  finishedAt: string;
  finalStandings: ArenaParticipantProgressResponse[];
}

// ── Spectator payloads ───────────────────────────────────────

export interface SpectatorFeedEvent {
  participantId: string;
  username: string;
  challengePosition: number;
  submissionStatus: BattleSubmissionStatus;
  attemptNumber: number;
  delayedAtTimestamp: number;
}

// ── User-specific submission result ──────────────────────────

export interface SubmissionResultResponse {
  submissionId: string;
  roomChallengeId: string;
  status: string;
  attemptNumber: number;
  runtimeMs: number | null;
  memoryKb: number | null;
  feedback: string;
  isAccepted: boolean;
}

// ── Request DTOs ────────────────────────────────────────────

export interface CreateRoomRequest {
  mode: string;
  maxPlayers: number;
  challengeCount: number;
  isPublic: boolean;
  difficulty: string;
}

export interface JoinRoomRequest {
  inviteToken: string;
  role?: string;
}

export interface ReadyToggleRequest {
  ready: boolean;
}

export interface KickParticipantRequest {
  targetUserId: string;
  reason?: string;
}

export interface SubmitSolutionRequest {
  roomId: string;
  roomChallengeId: string;
  language: string;
  code: string;
}

// ── Response DTOs ───────────────────────────────────────────

export interface RoomCreatedResponse {
  room: BattleRoomResponse;
  invite: InviteLinkResponse;
}

export interface InviteLinkResponse {
  inviteToken: string;
  inviteUrl: string;
  expiresAt: string;
}

export interface ScoreBreakdownResponse {
  roomChallengeId: string;
  challengePosition: number;
  challengeTitle: string;
  solved: boolean;
  correctnessScore: number;
  speedScore: number;
  efficiencyScore: number;
  attemptPenalty: number;
  totalChallengeScore: number;
  attemptCount: number;
  bestRuntimeMs: number | null;
  bestMemoryKb: number | null;
  solvedInSeconds: number;
}

export interface PlayerScoreResponse {
  participantId: string;
  userId: string;
  username: string;
  avatarUrl: string;
  finalRank: number;
  finalScore: number;
  eloChange: number;
  newElo: number;
  newTier: string;
  challengeBreakdowns: ScoreBreakdownResponse[];
  badgesAwarded: string[];
  isWinner: boolean;
}

export interface PostMatchSummaryResponse {
  roomId: string;
  mode: string;
  challengeCount: number;
  startedAt: string;
  finishedAt: string;
  durationSeconds: number;
  finishReason: string;
  standings: PlayerScoreResponse[];
  maxPossibleScore: number;
}
