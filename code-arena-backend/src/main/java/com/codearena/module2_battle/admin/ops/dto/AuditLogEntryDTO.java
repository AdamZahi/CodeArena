package com.codearena.module2_battle.admin.ops.dto;

import java.time.LocalDateTime;

public record AuditLogEntryDTO(
        String id,
        String adminId,
        String adminUsername,
        String action,
        String targetRoomId,
        String details,
        LocalDateTime performedAt
) {}
