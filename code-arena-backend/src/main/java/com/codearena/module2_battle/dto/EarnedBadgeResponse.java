package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarnedBadgeResponse {
    private String badgeId;
    private String key;
    private String name;
    private String description;
    private String iconUrl;
    private LocalDateTime awardedAt;
    private String roomId;
}
