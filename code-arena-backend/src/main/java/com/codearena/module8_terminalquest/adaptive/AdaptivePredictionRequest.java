package com.codearena.module8_terminalquest.adaptive;

import lombok.Data;

@Data
public class AdaptivePredictionRequest {
    private double successRate;
    private double avgAttempts;
    private double avgResponseTime;
    private int commandCategory;
    private int difficulty;
    private int streak;
}
