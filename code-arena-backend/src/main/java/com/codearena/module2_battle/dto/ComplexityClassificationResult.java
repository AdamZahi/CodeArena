package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result returned by the Python complexity-classifier microservice for a
 * single submission. Carries the predicted Big-O label, its display form
 * (e.g. "O(n log n)"), the 0–100 complexity score derived from that label,
 * and the model's softmax confidence. {@code fallback=true} means the bridge
 * produced this without reaching the Python service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityClassificationResult {

    /** Internal label key — one of O1, Ologn, On, Onlogn, On2, O2n. Null when unknown. */
    private String label;

    /** Pretty form for UI display, e.g. "O(n^2)". Null when label is null. */
    private String display;

    /** 0–100 complexity score derived from the label. 0.0 when unknown. */
    private double score;

    /** Softmax confidence of the predicted label, in [0, 1]. */
    private double confidence;

    /** Non-null when classification failed; explains why. */
    private String error;

    /** True when this result was synthesized locally because the classifier was unreachable. */
    private boolean fallback;
}
