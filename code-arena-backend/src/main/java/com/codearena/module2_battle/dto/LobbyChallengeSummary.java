package com.codearena.module2_battle.dto;

public record LobbyChallengeSummary(
    int index,
    String difficulty,
    String category,
    String titleSlug
) {}
