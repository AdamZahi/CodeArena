export interface SeasonLeaderboardEntry {
  rank: number;
  userId: string;
  username: string;
  avatarUrl: string;
  elo: number;
  tier: string;
  wins: number;
  losses: number;
  winStreak: number;
}

export interface SeasonLeaderboardResponse {
  seasonId: string;
  seasonName: string;
  seasonEndsAt: string;
  daysRemaining: number;
  totalEntries: number;
  page: number;
  size: number;
  entries: SeasonLeaderboardEntry[];
  requestingUserRank: number | null;
  requestingUserEntry: SeasonLeaderboardEntry | null;
}

export interface XpLeaderboardEntry {
  rank: number;
  userId: string;
  username: string;
  avatarUrl: string;
  totalXp: number;
  level: number;
  title: string;
}

export interface XpLeaderboardResponse {
  totalEntries: number;
  page: number;
  size: number;
  entries: XpLeaderboardEntry[];
  requestingUserRank: number | null;
  requestingUserEntry: XpLeaderboardEntry | null;
}
