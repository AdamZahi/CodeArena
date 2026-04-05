package com.codearena.module2_battle.exception;

public class RoomNotJoinableException extends RuntimeException {
    public RoomNotJoinableException(String roomId, String status) {
        super("Room " + roomId + " is not accepting new participants (status: " + status + ")");
    }
}
