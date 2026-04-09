package com.codearena.module2_battle.exception;

import com.codearena.module2_battle.enums.BattleRoomStatus;

public class ResultsNotReadyException extends RuntimeException {

    public ResultsNotReadyException(String roomId, BattleRoomStatus status) {
        super("Results for room " + roomId + " are not available yet — match is still " + status);
    }
}
