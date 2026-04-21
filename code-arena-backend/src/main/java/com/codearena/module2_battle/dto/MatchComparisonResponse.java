package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full post-match transparency view returned by
 * {@code GET /api/battle/results/{roomId}/compare}.
 *
 * <p>Lets every participant see how the winner was decided: per-challenge
 * cross-player metrics, the accepted source code, and the scoring formula
 * used to compute the final ranking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchComparisonResponse {
    private String roomId;
    private String mode;
    private long durationSeconds;

    /** Final standings (rank/score/username) — same shape as scoreboard. */
    private List<PlayerScoreResponse> standings;

    /** One entry per challenge, in arena order. */
    private List<ChallengeComparisonResponse> challenges;

    /** Human-readable formula text shown verbatim under the comparison. */
    private List<String> scoringFormulaLines;

    /** Whether ranker AI scoring data was available for at least one submission. */
    private boolean aiScoringAvailable;
}
