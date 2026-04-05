package com.codearena.module2_battle.exception;

public class BattleRoomFullException extends RuntimeException {

    public BattleRoomFullException(String roomId, int maxPlayers) {
        super("Battle room is full: roomId=" + roomId + ", maxPlayers=" + maxPlayers);
    }
}
