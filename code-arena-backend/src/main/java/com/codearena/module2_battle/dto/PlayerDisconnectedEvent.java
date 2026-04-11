package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDisconnectedEvent {
    private String participantId;
    private String displayName;
    private int reconnectDeadlineSeconds;
}
