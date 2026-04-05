package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayResponse {
    private String roomId;
    private List<ArenaChallengeResponse> challenges;
    private List<ReplaySubmissionResponse> timeline;
    private List<PlayerScoreResponse> finalStandings;
    private long totalDurationSeconds;
}
