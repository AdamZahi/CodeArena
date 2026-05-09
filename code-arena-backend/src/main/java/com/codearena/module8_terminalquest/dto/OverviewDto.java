package com.codearena.module8_terminalquest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OverviewDto {
    private long totalPlayers;
    private long totalMissionAttempts;
    private long totalMissionCompletions;
    private long totalSurvivalSessions;
    private double overallCompletionRate;
    private List<DifficultyStatDto> difficultyBreakdown;
}
