package com.codearena.module2_battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerScoreResponse {
    private String participantId;
    private String userId;
    private String username;
    private String avatarUrl;
    private int finalRank;
    private int finalScore;
    private int eloChange;
    private int newElo;
    private String newTier;
    private Integer eloBefore;
    private Integer eloAfter;
    private String tierBefore;
    private String tierAfter;
    @JsonProperty("tierChanged")
    private boolean tierChanged;
    private List<ScoreBreakdownResponse> challengeBreakdowns;
    private List<String> badgesAwarded;
    @JsonProperty("isWinner")
    private boolean isWinner;
}
