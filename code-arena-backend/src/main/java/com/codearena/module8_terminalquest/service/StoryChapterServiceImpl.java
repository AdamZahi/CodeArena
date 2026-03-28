package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.CreateStoryChapterRequest;
import com.codearena.module8_terminalquest.dto.StoryChapterDto;
import com.codearena.module8_terminalquest.dto.StoryLevelDto;
import com.codearena.module8_terminalquest.entity.StoryChapter;
import com.codearena.module8_terminalquest.repository.StoryChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryChapterServiceImpl implements StoryChapterService {

    private final StoryChapterRepository storyChapterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StoryChapterDto> getAllChapters() {
        return storyChapterRepository.findAllByOrderByOrderIndexAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StoryChapterDto getChapterById(UUID id) {
        StoryChapter chapter = storyChapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + id));
        return toDto(chapter);
    }

    @Override
    @Transactional
    public StoryChapterDto createChapter(CreateStoryChapterRequest request) {
        StoryChapter chapter = StoryChapter.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .orderIndex(request.getOrderIndex())
                .isLocked(request.isLocked())
                .build();
        return toDto(storyChapterRepository.save(chapter));
    }

    @Override
    @Transactional
    public StoryChapterDto updateChapter(UUID id, CreateStoryChapterRequest request) {
        StoryChapter chapter = storyChapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + id));
        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        chapter.setOrderIndex(request.getOrderIndex());
        chapter.setLocked(request.isLocked());
        return toDto(storyChapterRepository.save(chapter));
    }

    @Override
    @Transactional
    public void deleteChapter(UUID id) {
        storyChapterRepository.deleteById(id);
    }

    private StoryChapterDto toDto(StoryChapter chapter) {
        List<StoryLevelDto> levelDtos = chapter.getLevels().stream()
                .map(level -> StoryLevelDto.builder()
                        .id(level.getId())
                        .chapterId(chapter.getId())
                        .title(level.getTitle())
                        .scenario(level.getScenario())
                        .hint(level.getHint())
                        .orderIndex(level.getOrderIndex())
                        .difficulty(level.getDifficulty())
                        .isBoss(level.isBoss())
                        .xpReward(level.getXpReward())
                        .createdAt(level.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return StoryChapterDto.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .orderIndex(chapter.getOrderIndex())
                .isLocked(chapter.isLocked())
                .createdAt(chapter.getCreatedAt())
                .levels(levelDtos)
                .build();
    }
}
