package com.codearena.module8_terminalquest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryLevelDto {
    private UUID id;
    private UUID chapterId;
    private String title;
    private String scenario;
    private String hint;
    private int orderIndex;
    private String difficulty;
    private boolean isBoss;
    private int xpReward;
    private Instant createdAt;
    // acceptedAnswers intentionally excluded from response (server-side validation only)
}
