package com.codearena.module8_terminalquest.tts;

import lombok.Data;

@Data
public class TtsRequest {
    private String text;
    private String voiceName = "Charon";
    private String style;
}
