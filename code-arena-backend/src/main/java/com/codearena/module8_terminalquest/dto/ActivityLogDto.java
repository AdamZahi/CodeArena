package com.codearena.module8_terminalquest.dto;

import com.codearena.module8_terminalquest.entity.ActivityType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ActivityLogDto {
    private UUID id;
    private String userId;
    private ActivityType activityType;
    private String metadata;
    private Instant createdAt;
}
