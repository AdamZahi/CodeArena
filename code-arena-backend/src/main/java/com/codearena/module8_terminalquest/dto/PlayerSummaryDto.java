package com.codearena.module8_terminalquest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerSummaryDto {
    private String userId;
    private long totalAttempts;
    private long completedMissions;
    private int totalStars;
    private double completionRate;
}
