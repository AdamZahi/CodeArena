package com.codearena.module1_challenge.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {
    private Long challengeId;
    private String title;
    private String difficulty; // The original difficulty (EASY, MEDIUM, HARD)
    private String tags;
    private Float aiDifficultyScore;
    private Float matchScore; // 0 to 100 percentage of match
    private String reason; // E.g., "Strengthens your weak area: Arrays"
}
