package com.codearena.module2_battle.admin.ops.dto;

import java.time.Instant;

public record StuckRoomDTO(
        String roomId,
        String hostId,
        String hostUsername,
        String mode,
        Instant createdAt,
        long minutesStuck,
        int participantCount
) {}
