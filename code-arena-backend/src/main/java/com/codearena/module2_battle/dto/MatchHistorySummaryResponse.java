package com.codearena.module2_battle.dto;

import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MatchHistorySummaryResponse {
    private String roomId;
    private String mode;
    private BattleRoomStatus status;
    private LocalDateTime playedAt;
    private long durationSeconds;
    private int finalRank;
    private int finalScore;
    private int totalPlayers;
    private int eloChange;
    @JsonProperty("isWinner")
    private boolean isWinner;
    private String opponentSummary;
    private List<String> badgesEarned;
}
