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
public class ArenaChallengeResponse {
    private String roomChallengeId;
    private int position;
    private String challengeId;
    private String title;
    private String description;
    private String difficulty;
    private String tags;
    private List<VisibleTestCaseResponse> visibleTestCases;
}
