package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleStatsResponse {
    private long totalRoomsCreated;
    private long totalRoomsFinished;
    private long totalRoomsCancelled;
    private long totalSubmissions;
    private long totalAcceptedSubmissions;
    private double globalAcceptanceRate;
    private Map<String, Long> submissionsByLanguage;
    private Map<String, Long> roomsByMode;
    private long activeDailyStreakPlayers;
    private long totalDailyEntriesAllTime;
}
