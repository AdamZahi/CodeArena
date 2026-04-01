package com.codearena.module2_battle.dto;

import com.codearena.module2_battle.enums.BattleRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaStateResponse {
    private String roomId;
    private BattleRoomStatus status;
    private List<ArenaChallengeResponse> challenges;
    private List<ArenaParticipantProgressResponse> participants;
    private long remainingSeconds;
}
