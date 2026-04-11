package com.codearena.module2_battle.exception;

public class Judge0UnavailableException extends RuntimeException {

    public Judge0UnavailableException(Throwable cause) {
        super("Code execution service is temporarily unavailable — please retry", cause);
    }
}
