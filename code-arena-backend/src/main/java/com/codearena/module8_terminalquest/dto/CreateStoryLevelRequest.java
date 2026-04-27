package com.codearena.module8_terminalquest.dto;

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
    private UUID chapterId;
    private String title;
    private String scenario;
    private String acceptedAnswers; // JSON array string: ["cmd1", "cmd2"]
    private String hint;
    private int orderIndex;
    private String difficulty; // EASY, MEDIUM, HARD
    private boolean isBoss;
    private int xpReward;
}
