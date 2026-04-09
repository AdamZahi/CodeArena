package com.codearena.module2_battle.dto;

import java.time.Instant;
import java.util.List;

public record SharedResultDTO(
        String modeName,
        Instant matchFinishedAt,
        List<SharedParticipantResult> standings
) {}
