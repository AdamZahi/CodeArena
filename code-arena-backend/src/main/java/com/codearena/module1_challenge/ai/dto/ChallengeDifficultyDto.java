package com.codearena.module1_challenge.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDifficultyDto {
    private Long challengeId;
    private Float aiDifficultyScore;
    private Float passRate;
    private Float avgAttempts;
    private String humanDifficulty;
}
