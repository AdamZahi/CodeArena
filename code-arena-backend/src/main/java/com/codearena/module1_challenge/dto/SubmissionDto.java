package com.codearena.module1_challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDto {
    private Long id;
    private Long challengeId;
    private String userId;
    private String code;
    private String language;
    private String status;
    private String xpEarned;
    private Instant submittedAt;
    private Float executionTime;
    private Float memoryUsed;
    private String errorOutput;
    private String challengeTitle;

    /** Big-O class predicted by the complexity classifier (e.g. "O1", "Onlogn"). */
    private String complexityLabel;

    /** Pretty form of the predicted Big-O class (e.g. "O(n log n)"). */
    private String complexityDisplay;

    /** 0–100 score derived from the predicted Big-O class. */
    private Double complexityScore;

    /** Softmax confidence of the predicted class, in [0, 1]. */
    private Double complexityConfidence;
}
