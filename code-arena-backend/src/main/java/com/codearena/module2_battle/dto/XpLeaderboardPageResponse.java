package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpLeaderboardPageResponse {
    private int totalEntries;
    private int page;
    private int size;
    private List<XpLeaderboardEntryResponse> entries;
    private Integer requestingUserRank;
    private XpLeaderboardEntryResponse requestingUserEntry;
}
