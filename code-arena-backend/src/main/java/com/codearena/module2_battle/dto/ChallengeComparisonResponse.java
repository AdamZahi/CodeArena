package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One challenge worth of comparison data: every player's attempt on that
 * challenge, ordered by total score DESC. The frontend renders one tab/card
 * per challenge and lists each player's metrics side by side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeComparisonResponse {
    private String roomChallengeId;
    private int position;
    private String title;
    private String difficulty;
    private List<PlayerChallengeAttemptResponse> attempts;
}
