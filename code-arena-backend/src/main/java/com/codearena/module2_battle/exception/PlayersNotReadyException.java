package com.codearena.module2_battle.exception;

import java.util.List;

public class PlayersNotReadyException extends RuntimeException {
    public PlayersNotReadyException(List<String> unreadyUsernames) {
        super("Cannot start: players not ready: " + String.join(", ", unreadyUsernames));
    }
}
