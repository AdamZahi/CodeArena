package com.codearena.module8_terminalquest.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStat {
    private int completed;
    private int totalAttempts;
    private int totalStars;
    private double avgTime;
}
