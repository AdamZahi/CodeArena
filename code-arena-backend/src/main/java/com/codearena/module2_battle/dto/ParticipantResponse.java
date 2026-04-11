package com.codearena.module2_battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {
    private String participantId;
    private String userId;
    private String username;
    private String avatarUrl;
    private String role;
    @JsonProperty("isReady")
    private boolean isReady;
    private Integer currentElo;
    private String tier;
}
