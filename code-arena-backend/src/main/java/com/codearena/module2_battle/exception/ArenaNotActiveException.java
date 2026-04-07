package com.codearena.module2_battle.exception;

import com.codearena.module2_battle.enums.BattleRoomStatus;

public class ArenaNotActiveException extends RuntimeException {

    public ArenaNotActiveException(String roomId, BattleRoomStatus status) {
        super("Room " + roomId + " is not currently in an active battle (status: " + status + ")");
    }
}
