package com.codearena.module8_terminalquest.adaptive;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdaptivePredictionResponse {
    private double successProbability;
    private String recommendedAction;
    private int timerAdjustment;
    private boolean showHint;
    private String difficultyLabel;
    private String playerLevel;
}
