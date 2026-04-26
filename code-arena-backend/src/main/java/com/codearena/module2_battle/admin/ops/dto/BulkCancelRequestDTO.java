package com.codearena.module2_battle.admin.ops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkCancelRequestDTO(@NotEmpty List<String> roomIds, @NotBlank String reason) {}
