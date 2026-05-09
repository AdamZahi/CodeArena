package com.codearena.module8_terminalquest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DifficultyStatDto {
    private String difficulty;
    private long totalAttempts;
    private long completions;
    private double completionRate;
}
