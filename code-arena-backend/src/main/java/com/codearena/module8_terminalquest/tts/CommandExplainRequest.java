package com.codearena.module8_terminalquest.tts;

import lombok.Data;

@Data
public class CommandExplainRequest {
    private String command;
    private String missionTask;
    private String missionContext;
    private String difficulty;
    private boolean isCorrect;
}
