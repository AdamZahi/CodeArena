package com.codearena.module2_battle.admin.ops.dto;

import java.util.List;

public record BulkCancelResultDTO(int requested, int cancelled, List<String> notFound) {}
