package com.codearena.module2_battle.exception;

public class NotEnoughChallengesException extends RuntimeException {
    public NotEnoughChallengesException(String difficulty, int required, int available) {
        super("Not enough challenges available for difficulty " + difficulty
                + ". Required: " + required + ", available: " + available);
    }
}
