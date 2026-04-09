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
public class LeaderboardPageResponse {
    private String seasonId;
    private String seasonName;
    private LocalDateTime seasonEndsAt;
    private long daysRemaining;
    private int totalEntries;
    private int page;
    private int size;
    private List<SeasonLeaderboardEntryResponse> entries;
    private Integer requestingUserRank;
    private SeasonLeaderboardEntryResponse requestingUserEntry;
}
