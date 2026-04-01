package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonTopFinisherResponse {
    private int rank;
    private String userId;
    private String username;
    private String avatarUrl;
    private int finalElo;
    private String finalTier;
    private int wins;
    private int losses;
}
