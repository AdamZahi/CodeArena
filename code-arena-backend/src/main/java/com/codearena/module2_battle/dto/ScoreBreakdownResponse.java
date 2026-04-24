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

    /** Optimization score (0–100) from the Score Ranker for the accepted submission. */
    private Double aiScore;

    /** True when aiScore came from the local fallback (ranker unreachable). */
    private Boolean aiScoreFallback;

    /** Big-O class predicted by the complexity classifier (e.g. "O1", "Onlogn"). */
    private String complexityLabel;

    /** Pretty form of the predicted Big-O class (e.g. "O(n log n)"). */
    private String complexityDisplay;

    /** 0–100 score derived from the predicted Big-O class. */
    private Double complexityScore;
}
