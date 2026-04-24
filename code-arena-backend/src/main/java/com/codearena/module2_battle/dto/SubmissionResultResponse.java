package com.codearena.module2_battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultResponse {
    private String submissionId;
    private String roomChallengeId;
    private String status;
    private int attemptNumber;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String feedback;

    @JsonProperty("isAccepted")
    private boolean isAccepted;

    /** Optimization score (0–100) from the Score Ranker; null when not scored. */
    private Double aiScore;

    /** True when aiScore came from the local fallback (ranker unreachable). */
    private Boolean aiScoreFallback;

    /** Big-O class predicted by the complexity classifier (e.g. "O1", "Onlogn"). */
    private String complexityLabel;

    /** Pretty form of the predicted Big-O class (e.g. "O(n log n)"). */
    private String complexityDisplay;

    /** 0–100 score derived from the predicted Big-O class. */
    private Double complexityScore;

    /** Softmax confidence of the predicted class, in [0, 1]. */
    private Double complexityConfidence;
}
