package com.codearena.module8_terminalquest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryLevelRequest {
    @NotNull(message = "Chapter ID is required")
    private UUID chapterId;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Scenario is required")
    private String scenario;
    private String acceptedAnswers; // JSON array string: ["cmd1", "cmd2"]
    private String hint;
    @Min(value = 1, message = "Order index must be at least 1")
    private int orderIndex;
    @NotBlank(message = "Difficulty is required")
    private String difficulty; // EASY, MEDIUM, HARD
    private boolean isBoss;
    @Min(value = 1, message = "XP reward must be at least 1")
    private int xpReward;
}
