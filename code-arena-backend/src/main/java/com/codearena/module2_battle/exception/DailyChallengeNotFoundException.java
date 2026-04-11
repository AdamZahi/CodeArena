package com.codearena.module2_battle.exception;

import java.time.LocalDate;

public class DailyChallengeNotFoundException extends RuntimeException {
    public DailyChallengeNotFoundException(LocalDate date) {
        super("No daily challenge found for date " + date);
    }
}
