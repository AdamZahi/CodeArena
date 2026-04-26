package com.codearena.module2_battle.admin.ops.dto;

import jakarta.validation.constraints.NotBlank;

public record ForceEndRequestDTO(String winnerId, @NotBlank String reason) {}
