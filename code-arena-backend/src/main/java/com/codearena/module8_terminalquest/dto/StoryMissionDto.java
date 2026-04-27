package com.codearena.module8_terminalquest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class StoryMissionDto {
    private UUID id;
    private UUID chapterId;
    private String title;
    private String context;
    private String task;
    private String hint;
    private int orderIndex;
    private String difficulty;
    @JsonProperty("isBoss")
    private boolean isBoss;
    private int xpReward;
    private String speakerName;
    private String speakerVoice;
    private Instant createdAt;
    // acceptedAnswers intentionally excluded (server-side validation only)
}
