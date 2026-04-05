package com.codearena.module2_battle.exception;

import com.codearena.module2_battle.enums.BattleRoomStatus;

public class InvalidRoomTransitionException extends RuntimeException {

    public InvalidRoomTransitionException(BattleRoomStatus from, BattleRoomStatus to) {
        super("Invalid room transition from " + from + " to " + to);
    }

    public InvalidRoomTransitionException(BattleRoomStatus from, BattleRoomStatus to, String reason) {
        super("Invalid room transition from " + from + " to " + to + ": " + reason);
    }
}
