package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostMatchSummaryResponse {
    private String roomId;
    private String mode;
    private int challengeCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private long durationSeconds;
    private String finishReason;
    private List<PlayerScoreResponse> standings;
    private int maxPossibleScore;
}
