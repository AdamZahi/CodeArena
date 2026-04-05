package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLeaderboardResponse {
    private LocalDate date;
    private List<String> challengeTitles;
    private List<DailyLeaderboardEntryResponse> entries;
    private Integer requestingUserRank;
    private int totalParticipants;
}
