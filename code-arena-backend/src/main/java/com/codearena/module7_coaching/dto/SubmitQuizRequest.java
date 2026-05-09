package com.codearena.module7_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizRequest {
    @NotNull(message = "L'ID du quiz est obligatoire")
    private UUID quizId;
    
    /** Map of questionId -> userAnswer */
    @NotNull(message = "Les réponses sont obligatoires")
    @NotEmpty(message = "Les réponses ne peuvent pas être vides")
    private Map<UUID, String> answers;
}
