package com.codearena.module8_terminalquest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurvivalAnswerResponse {
    private boolean correct;
    private int livesRemaining;
    private int waveReached;
    private int score;
    private boolean gameOver;
    private String message;
    // Next challenge to display (null if gameOver)
    private StoryLevelDto nextChallenge;
}
