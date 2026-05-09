package com.codearena.module2_battle.admin.management.dto;

import com.codearena.module2_battle.enums.BattleRoomStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRoomStatusRequest(@NotNull BattleRoomStatus status, String reason) {}
