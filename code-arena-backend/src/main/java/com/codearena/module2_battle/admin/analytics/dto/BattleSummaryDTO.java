package com.codearena.module2_battle.admin.analytics.dto;

public record BattleSummaryDTO(
        long totalBattles,
        long activeBattles,
        long completedBattles,
        long abandonedBattles,
        double avgDurationMinutes,
        double globalWinRate,
        long totalParticipants
) {}
