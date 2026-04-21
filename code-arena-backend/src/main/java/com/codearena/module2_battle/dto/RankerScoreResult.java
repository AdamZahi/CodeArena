package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result returned by the Python ranker microservice for a single submission.
 * Carries the optimization score (0–100) plus the feature breakdown the model
 * used. {@code fallback=true} means the bridge produced this without reaching
 * the Python service (typically a degraded time-based score).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankerScoreResult {

    private double score;
    private double timeMs;
    private double memoryKb;
    private double testsRatio;

    /** Non-null when the ranker failed and we returned a fallback estimate. */
    private String error;

    /** True when the score came from the Java-side fallback (ranker unreachable). */
    private boolean fallback;
}
