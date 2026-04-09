package com.codearena.module2_battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotBlank
    private String mode; // DUEL | TEAM | RANKED_ARENA | BLITZ | PRACTICE | DAILY

    @Min(2)
    @Max(10)
    private int maxPlayers;

    @Min(1)
    @Max(10)
    private int challengeCount;

    @JsonProperty("isPublic")
    private boolean isPublic;

    private String difficulty; // EASY | MEDIUM | HARD | MIXED
}
