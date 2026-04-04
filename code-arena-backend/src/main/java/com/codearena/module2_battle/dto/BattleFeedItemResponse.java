package com.codearena.module2_battle.dto;

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
public class BattleFeedItemResponse {
    private String roomId;
    private String mode;
    private String status;
    @JsonProperty("isLive")
    private boolean isLive;
    private int playerCount;
    private int maxPlayers;
    private int spectatorCount;
    private String hostUsername;
    private List<String> playerUsernames;
    private LocalDateTime createdAt;
    private long secondsAgo;
    private String modeBadge;
}
