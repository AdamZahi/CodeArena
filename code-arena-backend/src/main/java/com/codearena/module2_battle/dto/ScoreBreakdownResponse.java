package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownResponse {
    private String roomChallengeId;
    private int challengePosition;
    private String challengeTitle;
    private boolean solved;
    private int correctnessScore;
    private int speedScore;
    private int efficiencyScore;
    private int attemptPenalty;
    private int totalChallengeScore;
    private int attemptCount;
    private Integer bestRuntimeMs;
    private Integer bestMemoryKb;
    private long solvedInSeconds;
}
