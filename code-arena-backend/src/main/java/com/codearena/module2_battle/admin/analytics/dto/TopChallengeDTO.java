package com.codearena.module2_battle.admin.analytics.dto;

public record TopChallengeDTO(
        String challengeId,
        String title,
        long timesUsed,
        String difficulty
) {}
