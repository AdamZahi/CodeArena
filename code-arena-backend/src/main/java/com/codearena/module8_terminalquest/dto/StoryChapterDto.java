package com.codearena.module8_terminalquest.dto;

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
    private boolean isLocked;
    private Instant createdAt;
    private List<StoryLevelDto> levels;
}
