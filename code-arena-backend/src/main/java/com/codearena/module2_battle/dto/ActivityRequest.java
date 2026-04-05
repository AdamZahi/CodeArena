package com.codearena.module2_battle.dto;

import com.codearena.module2_battle.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequest {
    private ActivityType type;
    private Long challengeId;
}
