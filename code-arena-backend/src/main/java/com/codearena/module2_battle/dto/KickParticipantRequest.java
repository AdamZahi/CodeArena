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
public class KickParticipantRequest {

    @NotBlank
    private String targetUserId;

    private String reason;
}
