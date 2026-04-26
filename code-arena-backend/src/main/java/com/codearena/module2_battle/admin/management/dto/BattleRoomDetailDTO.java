package com.codearena.module2_battle.admin.management.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record BattleRoomDetailDTO(
        String id,
        String hostId,
        String hostUsername,
        String mode,
        Integer maxPlayers,
        Integer challengeCount,
        String inviteToken,
        Boolean isPublic,
        String status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Instant createdAt,
        List<String> challengeIds,
        List<BattleParticipantAdminDTO> participants,
        String winnerId
) {}
