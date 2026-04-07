package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDto {
    private UUID id;
    private String title;
    private String description;
    private QuizDifficulty difficulty;
    private ProgrammingLanguage language;
    private Integer totalPoints;
    private String category;
    private String createdBy;
    private Instant createdAt;
    private List<QuestionDto> questions;
}
