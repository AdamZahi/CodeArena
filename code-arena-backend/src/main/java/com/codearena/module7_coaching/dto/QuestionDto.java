package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.QuestionType;
import com.codearena.module7_coaching.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private UUID id;
    private UUID quizId;
    private String content;
    private QuestionType type;
    private ProgrammingLanguage language;
    private QuizDifficulty difficulty;
    private Integer points;
    private String correctAnswer;
    private String explanation;
    private String codeSnippet;
    private String options;
}
