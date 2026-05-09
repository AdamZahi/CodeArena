package com.codearena.module2_battle.admin.ops.dto;

import jakarta.validation.constraints.NotBlank;

public record ReassignWinnerRequestDTO(@NotBlank String newWinnerId, @NotBlank String reason) {}
