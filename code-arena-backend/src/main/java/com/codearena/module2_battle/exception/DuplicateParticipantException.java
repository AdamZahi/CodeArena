package com.codearena.module2_battle.exception;

public class DuplicateParticipantException extends RuntimeException {

    public DuplicateParticipantException(String roomId, String userId) {
        super("User already in room: roomId=" + roomId + ", userId=" + userId);
    }
}
