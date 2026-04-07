package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardPageRequest {
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 50;
    private String tier;
    private String search;
}
