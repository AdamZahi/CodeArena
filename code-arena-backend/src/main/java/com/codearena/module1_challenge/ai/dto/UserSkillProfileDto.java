package com.codearena.module1_challenge.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillProfileDto {
    private String userId;
    private Map<String, Double> skillMap;
    private Float overallRating;
    private String strongestTag;
    private String weakestTag;
    private Integer totalSolved;
    private Integer totalAttempted;
}
