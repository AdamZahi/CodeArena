package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.CreateStoryLevelRequest;
import com.codearena.module8_terminalquest.dto.StoryLevelDto;

import java.util.List;
import java.util.UUID;

public interface StoryLevelService {
    List<StoryLevelDto> getLevelsByChapter(UUID chapterId);
    StoryLevelDto getLevelById(UUID id);
    StoryLevelDto createLevel(CreateStoryLevelRequest request);
    StoryLevelDto updateLevel(UUID id, CreateStoryLevelRequest request);
    void deleteLevel(UUID id);
}
