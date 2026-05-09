package com.codearena.module8_terminalquest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsDto {
    private String userId;
    private int totalLevelsCompleted;
    private int totalStarsEarned;
    private int totalAttempts;
    private int bestWave;
    private int bestScore;
    private int totalSurvivalSessions;
}
