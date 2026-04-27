package com.codearena.module8_terminalquest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class StoryChapterDto {
    private UUID id;
    private String title;
    private String description;
    private int orderIndex;
    @JsonProperty("isLocked")
    private boolean isLocked;
    private String speakerName;
    private String speakerVoice;
    private Instant createdAt;
    private List<StoryLevelDto> levels;
    private List<StoryMissionDto> missions;
}
