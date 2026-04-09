package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLeaderboardEntryResponse {
    private int rank;
    private String userId;
    private String username;
    private String avatarUrl;
    private int score;
    private int timeSeconds;
    private String tier;
    private int dailyStreak;
}
