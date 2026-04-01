package com.codearena.module2_battle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Time limit configuration per battle mode (in minutes).
 * -1 means no time limit (used for PRACTICE and DAILY).
 */
@Data
@Component
@ConfigurationProperties(prefix = "battle.time-limit")
public class TimeLimitProperties {
    private int duelMinutes = 30;
    private int teamMinutes = 40;
    private int rankedArenaMinutes = 25;
    private int blitzMinutes = 15;
    private int practiceMinutes = -1;
    private int dailyMinutes = -1;
}
