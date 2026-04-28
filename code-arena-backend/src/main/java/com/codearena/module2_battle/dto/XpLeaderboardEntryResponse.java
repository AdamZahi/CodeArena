package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpLeaderboardEntryResponse {
    private int rank;
    private String userId;
    private String username;
    private String avatarUrl;
    private long totalXp;
    private int level;
    private String title;
}
