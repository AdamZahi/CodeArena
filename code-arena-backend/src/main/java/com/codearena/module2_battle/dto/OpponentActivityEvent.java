package com.codearena.module2_battle.dto;

import com.codearena.module2_battle.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpponentActivityEvent {
    private String participantId;
    private String displayName;
    private ActivityType type;
    private Long challengeId;
    private Instant timestamp;
}
