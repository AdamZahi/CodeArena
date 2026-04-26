package com.codearena.module2_battle.admin.analytics.dto;

public record OutcomeDistributionDTO(
        long wins,
        long draws,
        long abandoned,
        double winRate,
        double drawRate,
        double abandonedRate
) {}
