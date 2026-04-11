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
public class SeasonSummaryResponse {
    private SeasonResponse season;
    private int totalParticipants;
    private List<SeasonTopFinisherResponse> topThree;
}
