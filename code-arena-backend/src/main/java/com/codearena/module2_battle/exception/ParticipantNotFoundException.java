package com.codearena.module2_battle.exception;

public class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(String roomId) {
        super("Participant not found in room " + roomId);
    }
}
