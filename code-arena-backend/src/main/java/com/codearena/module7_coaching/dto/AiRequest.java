package com.codearena.module7_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest {
    private String mode; // SESSION_PLAN, QUIZ_GENERATE, CHAT
    private String topic;
    private String language; // JAVA, PYTHON, etc.
    private String level; // BASIQUE, INTERMEDIAIRE, AVANCE
    private Integer durationMinutes;
    private Integer questionCount;
    private String message; // for chat mode
    private String context; // additional context
}
