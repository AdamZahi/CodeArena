package com.codearena.module2_battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One player's attempt on a single challenge, with full metrics and (when
 * solved) the accepted source code. Used by the post-match comparison view to
 * show participants exactly how their submission stacked up against everyone
 * else's.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerChallengeAttemptResponse {

    private String participantId;
    private String userId;
    private String username;
    private String avatarUrl;
    private int finalRank;

    private boolean solved;
    private int attemptCount;
    private long solvedInSeconds;

    private Integer runtimeMs;
    private Integer memoryKb;

    private Double aiScore;
    private Boolean aiScoreFallback;

    private int correctnessScore;
    private int speedScore;
    private int efficiencyScore;
    private int attemptPenalty;
    private int totalChallengeScore;

    /** True when this player had the lowest runtimeMs among solvers of this challenge. */
    @JsonProperty("isFastest")
    private boolean isFastest;

    /** True when this player had the highest aiScore among solvers of this challenge. */
    @JsonProperty("isMostOptimized")
    private boolean isMostOptimized;

    /** True when this player solved the challenge first (lowest solvedInSeconds). */
    @JsonProperty("isFirstSolver")
    private boolean isFirstSolver;

    /** Accepted source code — null when the player did not solve this challenge. */
    private String acceptedCode;

    /** Language of the accepted submission — null when not solved. */
    private String language;
}
