package com.codearena.module2_battle.exception;

public class ActiveSeasonNotFoundException extends RuntimeException {

    public ActiveSeasonNotFoundException() {
        super("No active season is currently configured — contact an administrator");
    }
}
