package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.CreateStoryLevelRequest;
import com.codearena.module8_terminalquest.dto.StoryLevelDto;
import com.codearena.module8_terminalquest.entity.StoryChapter;
import com.codearena.module8_terminalquest.entity.StoryLevel;
import com.codearena.module8_terminalquest.repository.StoryChapterRepository;
import com.codearena.module8_terminalquest.repository.StoryLevelRepository;
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
public class StoryLevelServiceImpl implements StoryLevelService {

    private final StoryLevelRepository storyLevelRepository;
    private final StoryChapterRepository storyChapterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StoryLevelDto> getLevelsByChapter(UUID chapterId) {
        return storyLevelRepository.findByChapterIdOrderByOrderIndexAsc(chapterId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StoryLevelDto getLevelById(UUID id) {
        StoryLevel level = storyLevelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Level not found: " + id));
        return toDto(level);
    }

    @Override
    @Transactional
    public StoryLevelDto createLevel(CreateStoryLevelRequest request) {
        StoryChapter chapter = storyChapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + request.getChapterId()));
        StoryLevel level = StoryLevel.builder()
                .chapter(chapter)
                .title(request.getTitle())
                .scenario(request.getScenario())
                .acceptedAnswers(request.getAcceptedAnswers())
                .hint(request.getHint())
                .orderIndex(request.getOrderIndex())
                .difficulty(request.getDifficulty())
                .isBoss(request.isBoss())
                .xpReward(request.getXpReward())
                .build();
        return toDto(storyLevelRepository.save(level));
    }

    @Override
    @Transactional
    public StoryLevelDto updateLevel(UUID id, CreateStoryLevelRequest request) {
        StoryLevel level = storyLevelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Level not found: " + id));
        if (request.getChapterId() != null) {
            StoryChapter chapter = storyChapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new RuntimeException("Chapter not found: " + request.getChapterId()));
            level.setChapter(chapter);
        }
        level.setTitle(request.getTitle());
        level.setScenario(request.getScenario());
        level.setAcceptedAnswers(request.getAcceptedAnswers());
        level.setHint(request.getHint());
        level.setOrderIndex(request.getOrderIndex());
        level.setDifficulty(request.getDifficulty());
        level.setBoss(request.isBoss());
        level.setXpReward(request.getXpReward());
        return toDto(storyLevelRepository.save(level));
    }

    @Override
    @Transactional
    public void deleteLevel(UUID id) {
        storyLevelRepository.deleteById(id);
    }

    StoryLevelDto toDto(StoryLevel level) {
        return StoryLevelDto.builder()
                .id(level.getId())
                .chapterId(level.getChapter().getId())
                .title(level.getTitle())
                .scenario(level.getScenario())
                .hint(level.getHint())
                .orderIndex(level.getOrderIndex())
                .difficulty(level.getDifficulty())
                .isBoss(level.isBoss())
                .xpReward(level.getXpReward())
                .createdAt(level.getCreatedAt())
                .build();
    }
}
