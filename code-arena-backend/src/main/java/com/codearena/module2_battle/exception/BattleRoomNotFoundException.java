package com.codearena.module2_battle.exception;

public class BattleRoomNotFoundException extends RuntimeException {

    public BattleRoomNotFoundException(String roomId) {
        super("Battle room not found: roomId=" + roomId);
    }
}
