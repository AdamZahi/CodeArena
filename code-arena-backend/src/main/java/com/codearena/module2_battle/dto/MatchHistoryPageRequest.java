package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchHistoryPageRequest {
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
    private String mode;
    private String result;
}
