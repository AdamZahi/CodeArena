package com.codearena.module2_battle.dto;

import com.codearena.module2_battle.enums.ProgressPulse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpponentProgressEvent {
    private String participantId;
    private String userId;
    private int challengesCompleted;
    private int currentChallengePosition;
    private int totalAttempts;
    @JsonProperty("isFinished")
    private boolean isFinished;
    private ProgressPulse pulse;
}
