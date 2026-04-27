package com.codearena.module8_terminalquest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryDto {
    private int rank;
    private String userId;
    private int bestWave;
    private int bestScore;
}
