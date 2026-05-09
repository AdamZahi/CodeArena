export type BattleRoomStatus = 'WAITING' | 'COUNTDOWN' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED';

export interface BattleSummary {
  totalBattles: number;
  activeBattles: number;
  completedBattles: number;
  abandonedBattles: number;
  avgDurationMinutes: number;
  globalWinRate: number;
  totalParticipants: number;
}

export interface TimelinePoint {
  period: string;
  count: number;
}

export interface TopChallenge {
  challengeId: string;
  title: string;
  timesUsed: number;
  difficulty: string | null;
}

export interface TopPlayer {
  userId: string;
  username: string;
  battlesPlayed: number;
  battlesWon: number;
  winRate: number;
  xpEarned: number;
}

export interface LanguageDistribution {
  language: string;
  count: number;
  percentage: number;
}

export interface OutcomeDistribution {
  wins: number;
  draws: number;
  abandoned: number;
  winRate: number;
  drawRate: number;
  abandonedRate: number;
}

export interface AvgDuration {
  avgDurationMinutes: number;
  sampleSize: number;
}

export interface BattleConfigDTO {
  maxParticipants: number;
  timeLimitMinutes: number;
  allowedLanguages: string[];
  xpRewardWinner: number;
  xpRewardLoser: number;
  minRankRequired: string | null;
  allowSpectators: boolean;
  autoCloseAbandonedAfterMinutes: number;
  updatedAt?: string | null;
  updatedBy?: string | null;
}

export interface BattleRoomAdmin {
  id: string;
  challengeId: string | null;
  challengeTitle: string | null;
  hostId: string | null;
  hostUsername: string | null;
  status: BattleRoomStatus;
  mode: string;
  roomKey: string | null;
  createdAt: string;
  participantCount: number;
  winnerId: string | null;
}

export interface BattleParticipantAdmin {
  id: string;
  userId: string | null;
  username: string | null;
  role: string;
  ready: boolean | null;
  score: number | null;
  rank: number | null;
  eloChange: number | null;
  joinedAt: string;
}

export interface BattleRoomDetail {
  id: string;
  hostId: string | null;
  hostUsername: string | null;
  mode: string;
  maxPlayers: number;
  challengeCount: number;
  inviteToken: string | null;
  isPublic: boolean;
  status: BattleRoomStatus;
  startsAt: string | null;
  endsAt: string | null;
  createdAt: string;
  challengeIds: string[];
  participants: BattleParticipantAdmin[];
  winnerId: string | null;
}

export interface StuckRoom {
  roomId: string;
  hostId: string | null;
  hostUsername: string | null;
  mode: string;
  createdAt: string;
  minutesStuck: number;
  participantCount: number;
}

export interface AuditLogEntry {
  id: string;
  adminId: string;
  adminUsername: string | null;
  action: string;
  targetRoomId: string | null;
  details: string | null;
  performedAt: string;
}

export interface BulkCancelResult {
  requested: number;
  cancelled: number;
  notFound: string[];
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
}

export interface RoomListFilters {
  status?: BattleRoomStatus | null;
  challengeId?: string | null;
  hostId?: string | null;
  from?: string | null;
  to?: string | null;
  page?: number;
  size?: number;
  sort?: string;
}
