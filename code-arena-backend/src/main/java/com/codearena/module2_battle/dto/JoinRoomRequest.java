package com.codearena.module2_battle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {

    @NotBlank
    private String inviteToken;

    private String role; // PLAYER | SPECTATOR — defaults to PLAYER if absent
}
