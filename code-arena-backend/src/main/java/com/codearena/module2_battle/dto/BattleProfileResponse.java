package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleProfileResponse {
    private String userId;
    private String username;
    private String avatarUrl;

    // Current season rating
    private Integer currentElo;
    private String currentTier;
    private int seasonWins;
    private int seasonLosses;
    private int seasonDraws;
    private int currentWinStreak;
    private int bestWinStreak;

    // All-time stats
    private int totalMatchesPlayed;
    private int totalRankedMatchesPlayed;
    private int totalWins;
    private int totalSolvedChallenges;
    private double averageScore;
    private int totalBattlePoints;

    // Badges
    private List<EarnedBadgeResponse> badges;

    // Recent matches
    private List<MatchHistorySummaryResponse> recentMatches;

    // Daily challenge streak
    private int dailyStreak;
    private int longestDailyStreak;
}
