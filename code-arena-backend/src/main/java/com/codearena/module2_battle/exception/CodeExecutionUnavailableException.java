package com.codearena.module2_battle.exception;

public class CodeExecutionUnavailableException extends RuntimeException {

    public CodeExecutionUnavailableException(Throwable cause) {
        super("Code execution service is temporarily unavailable — please retry", cause);
    }
}
