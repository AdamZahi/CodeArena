package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaParticipantProgressResponse {
    private String participantId;
    private String userId;
    private String username;
    private String avatarUrl;
    private int challengesCompleted;
    private int currentChallengePosition;
    private int totalAttempts;
    private boolean isFinished;
}
