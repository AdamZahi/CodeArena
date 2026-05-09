package com.codearena.module7_coaching.dto;

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
public class BadgeDto {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private Instant earnedAt;
}
