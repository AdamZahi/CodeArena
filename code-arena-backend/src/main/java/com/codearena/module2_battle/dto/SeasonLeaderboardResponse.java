package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonLeaderboardResponse {
    private String seasonId;
    private String seasonName;
    private LocalDateTime seasonEndsAt;
    private List<SeasonLeaderboardEntryResponse> entries;
    private Integer requestingUserRank;
}
