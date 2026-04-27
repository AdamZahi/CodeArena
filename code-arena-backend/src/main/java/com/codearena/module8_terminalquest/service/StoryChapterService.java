package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.CreateStoryChapterRequest;
import com.codearena.module8_terminalquest.dto.StoryChapterDto;

import java.util.List;
import java.util.UUID;

public interface StoryChapterService {
    List<StoryChapterDto> getAllChapters();
    StoryChapterDto getChapterById(UUID id);
    StoryChapterDto createChapter(CreateStoryChapterRequest request);
    StoryChapterDto updateChapter(UUID id, CreateStoryChapterRequest request);
    void deleteChapter(UUID id);
}
