package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonResponse {
    private String id;
    private String name;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean isActive;
}
