package com.codearena.module2_battle.admin.config.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record BattleConfigDTO(
        @Min(2) @NotNull Integer maxParticipants,
        @Min(1) @NotNull Integer timeLimitMinutes,
        @NotNull List<String> allowedLanguages,
        @Min(0) @NotNull Integer xpRewardWinner,
        @Min(0) @NotNull Integer xpRewardLoser,
        String minRankRequired,
        @NotNull Boolean allowSpectators,
        @Min(1) @NotNull Integer autoCloseAbandonedAfterMinutes,
        LocalDateTime updatedAt,
        String updatedBy
) {}
