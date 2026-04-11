export interface OpponentInfo {
  userId: string;
  username: string;
  avatarUrl: string;
}

export interface MatchHistorySummary {
  roomId: string;
  mode: string;
  status: string;
  playedAt: string;
  durationSeconds: number;
  finalRank: number;
  finalScore: number;
  totalPlayers: number;
  eloChange: number;
  isWinner: boolean;
  opponentSummary: string;
  opponents: OpponentInfo[];
  badgesEarned: string[];
}

export interface MatchHistoryResponse {
  userId: string;
  totalMatches: number;
  page: number;
  size: number;
  matches: MatchHistorySummary[];
}
