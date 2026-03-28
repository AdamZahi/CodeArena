package com.codearena.module8_terminalquest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterCompletionStatsDto {
    private UUID chapterId;
    private String chapterTitle;
    private int totalLevels;
    private long totalAttempts;     // total level_progress rows for this chapter
    private long totalCompletions;  // completed rows for this chapter
    private double completionRate;  // completions / attempts (0.0 if no attempts)
}
