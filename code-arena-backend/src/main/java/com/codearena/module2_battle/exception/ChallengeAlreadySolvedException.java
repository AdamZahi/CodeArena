package com.codearena.module2_battle.exception;

public class ChallengeAlreadySolvedException extends RuntimeException {

    public ChallengeAlreadySolvedException(int challengePosition) {
        super("Challenge " + challengePosition + " has already been solved — no resubmission allowed");
    }
}
