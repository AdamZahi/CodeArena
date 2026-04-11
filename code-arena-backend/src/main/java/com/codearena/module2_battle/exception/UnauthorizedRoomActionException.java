package com.codearena.module2_battle.exception;

public class UnauthorizedRoomActionException extends RuntimeException {
    public UnauthorizedRoomActionException() {
        super("Only the room host can perform this action");
    }
}
