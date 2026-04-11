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
public class MatchHistoryPageResponse {
    private String userId;
    private int totalMatches;
    private int page;
    private int size;
    private List<MatchHistorySummaryResponse> matches;
}
