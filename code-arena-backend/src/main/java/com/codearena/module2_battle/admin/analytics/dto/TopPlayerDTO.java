package com.codearena.module2_battle.admin.analytics.dto;

public record TopPlayerDTO(
        String userId,
        String username,
        long battlesPlayed,
        long battlesWon,
        double winRate,
        long xpEarned
) {}
