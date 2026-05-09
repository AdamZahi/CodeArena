package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.QuestionType;
import com.codearena.module7_coaching.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private UUID id;
    private UUID quizId;

    @NotBlank(message = "Le contenu de la question est obligatoire")
    private String content;

    @NotNull(message = "Le type de question est obligatoire")
    private QuestionType type;

    private ProgrammingLanguage language;
    private QuizDifficulty difficulty;

    @NotNull(message = "Le nombre de points est obligatoire")
    private Integer points;

    @NotBlank(message = "La réponse correcte est obligatoire")
    private String correctAnswer;

    private String explanation;
    private String codeSnippet;
    private String options;
}
