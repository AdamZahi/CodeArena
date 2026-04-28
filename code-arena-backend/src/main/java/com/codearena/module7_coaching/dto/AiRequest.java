package com.codearena.module7_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest {
    @NotBlank(message = "Le mode est obligatoire")
    private String mode; // SESSION_PLAN, QUIZ_GENERATE, CHAT
    
    @NotBlank(message = "Le sujet (topic) est obligatoire")
    private String topic;
    
    private String language; // JAVA, PYTHON, etc.
    private String level; // BASIQUE, INTERMEDIAIRE, AVANCE
    
    @Min(value = 1, message = "La durée doit être d'au moins 1 minute")
    private Integer durationMinutes;
    
    @Min(value = 1, message = "Le nombre de questions doit être d'au moins 1")
    @Max(value = 50, message = "Le nombre de questions ne peut pas dépasser 50")
    private Integer questionCount;
    
    private String message; // for chat mode
    private String context; // additional context
}
