package com.codearena.module2_battle.admin.management.dto;

import java.time.Instant;

public record BattleRoomAdminDTO(
        String id,
        String challengeId,
        String challengeTitle,
        String hostId,
        String hostUsername,
        String status,
        String mode,
        String roomKey,
        Instant createdAt,
        int participantCount,
        String winnerId
) {}
