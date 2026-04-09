package com.codearena.module2_battle.dto;

import com.codearena.module2_battle.enums.BattleSubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpectatorFeedEvent {
    private String participantId;
    private String username;
    private int challengePosition;
    private BattleSubmissionStatus submissionStatus;
    private int attemptNumber;
    private long delayedAtTimestamp;
}
