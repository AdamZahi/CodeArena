package com.codearena.module2_battle.event;

import com.codearena.module2_battle.enums.BattleRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BattleRoomStatusChangedEvent {
    private final String roomId;
    private final BattleRoomStatus previousStatus;
    private final BattleRoomStatus newStatus;
}
