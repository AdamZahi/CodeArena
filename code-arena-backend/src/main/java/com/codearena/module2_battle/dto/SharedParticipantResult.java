package com.codearena.module2_battle.dto;

import java.util.List;

public record SharedParticipantResult(
        String displayName,
        int rank,
        int totalScore,
        int challengesSolved,
        int totalChallenges,
        List<String> badgesEarned,
        Integer eloChange,
        String newTier
) {}
