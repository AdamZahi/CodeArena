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
public class LobbyStateResponse {
    private BattleRoomResponse room;
    private List<ParticipantResponse> players;
    private List<ParticipantResponse> spectators;
    private boolean canStart;
    private int countdownSeconds;
}
