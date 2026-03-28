package com.codearena.module8_terminalquest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelProgressDto {
    private UUID id;
    private String userId;
    private UUID levelId;
    private boolean completed;
    private int starsEarned;
    private int attempts;
    private String completedAt;
    private Instant createdAt;
}
