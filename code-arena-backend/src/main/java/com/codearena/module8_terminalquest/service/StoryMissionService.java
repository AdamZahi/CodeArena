package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.CreateStoryMissionRequest;
import com.codearena.module8_terminalquest.dto.StoryMissionDto;

import java.util.List;
import java.util.UUID;

public interface StoryMissionService {
    List<StoryMissionDto> getMissionsByChapter(UUID chapterId);
    StoryMissionDto getMissionById(UUID id);
    StoryMissionDto createMission(CreateStoryMissionRequest request);
    StoryMissionDto updateMission(UUID id, CreateStoryMissionRequest request);
    void deleteMission(UUID id);
}
