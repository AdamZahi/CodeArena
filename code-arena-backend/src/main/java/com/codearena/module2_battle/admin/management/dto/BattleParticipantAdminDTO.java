package com.codearena.module2_battle.admin.management.dto;

import java.time.Instant;

public record BattleParticipantAdminDTO(
        String id,
        String userId,
        String username,
        String role,
        Boolean ready,
        Integer score,
        Integer rank,
        Integer eloChange,
        Instant joinedAt
) {}
