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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDto {
    private UUID id;

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "La difficulté est obligatoire")
    private QuizDifficulty difficulty;

    @NotNull(message = "Le langage est obligatoire")
    private ProgrammingLanguage language;

    private Integer totalPoints;
    private String category;
    private String createdBy;
    private Instant createdAt;

    @NotEmpty(message = "Le quiz doit contenir au moins une question")
    @Valid
    private List<QuestionDto> questions;
}
