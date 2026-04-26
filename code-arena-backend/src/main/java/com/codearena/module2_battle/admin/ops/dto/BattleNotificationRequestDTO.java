package com.codearena.module2_battle.admin.ops.dto;

import jakarta.validation.constraints.NotBlank;

public record BattleNotificationRequestDTO(
        @NotBlank String title,
        @NotBlank String message
) {}
