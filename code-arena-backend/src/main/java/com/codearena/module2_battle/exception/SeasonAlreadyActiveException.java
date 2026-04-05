package com.codearena.module2_battle.exception;

public class SeasonAlreadyActiveException extends RuntimeException {
    public SeasonAlreadyActiveException() {
        super("A season is already active — close it before creating a new one");
    }
}
