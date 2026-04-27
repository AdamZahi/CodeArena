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
public class SurvivalLeaderboardDto {
    private UUID id;
    private String userId;
    private int bestWave;
    private int bestScore;
}
