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
public class BattleRoomResponse {
    private String id;
    private String mode;
    private String status;
    private int maxPlayers;
    private int challengeCount;
    private boolean isPublic;
    private String inviteToken;
    private String hostId;
    private LocalDateTime createdAt;
    private LocalDateTime startsAt;
    private List<ParticipantResponse> participants;
    private int spectatorCount;
}
