package com.codearena.module8_terminalquest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalStatsDto {
    private long totalActivePlayers;       // distinct userIds across story + survival
    private long totalSurvivalSessions;
    private long totalStoryAttempts;       // total level_progress rows
    private long totalStoryCompletions;    // completed level_progress rows
    private double overallCompletionRate;  // completions / attempts
    private List<ChapterCompletionStatsDto> chapterStats;
}
