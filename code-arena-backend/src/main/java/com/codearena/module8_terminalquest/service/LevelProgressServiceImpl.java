package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.LevelProgressDto;
import com.codearena.module8_terminalquest.dto.SubmitAnswerRequest;
import com.codearena.module8_terminalquest.dto.SubmitAnswerResponse;
import com.codearena.module8_terminalquest.entity.LevelProgress;
import com.codearena.module8_terminalquest.entity.StoryLevel;
import com.codearena.module8_terminalquest.repository.LevelProgressRepository;
import com.codearena.module8_terminalquest.repository.StoryLevelRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelProgressServiceImpl implements LevelProgressService {

    private final LevelProgressRepository levelProgressRepository;
    private final StoryLevelRepository storyLevelRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SubmitAnswerResponse submitAnswer(UUID levelId, SubmitAnswerRequest request) {
        StoryLevel level = storyLevelRepository.findById(levelId)
                .orElseThrow(() -> new RuntimeException("Level not found: " + levelId));

        LevelProgress progress = levelProgressRepository
                .findByUserIdAndLevelId(request.getUserId(), levelId)
                .orElseGet(() -> LevelProgress.builder()
                        .userId(request.getUserId())
                        .level(level)
                        .completed(false)
                        .starsEarned(0)
                        .attempts(0)
                        .build());

        progress.setAttempts(progress.getAttempts() + 1);

        boolean correct = isAnswerCorrect(request.getAnswer(), level.getAcceptedAnswers());

        if (correct && !progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(Instant.now().toString());
            int stars = calculateStars(progress.getAttempts());
            progress.setStarsEarned(stars);
            levelProgressRepository.save(progress);
            return SubmitAnswerResponse.builder()
                    .correct(true)
                    .starsEarned(stars)
                    .xpEarned(level.getXpReward())
                    .attempts(progress.getAttempts())
                    .message("Correct! You earned " + stars + " star(s) and " + level.getXpReward() + " XP.")
                    .build();
        }

        levelProgressRepository.save(progress);

        if (correct) {
            return SubmitAnswerResponse.builder()
                    .correct(true)
                    .starsEarned(progress.getStarsEarned())
                    .xpEarned(0)
                    .attempts(progress.getAttempts())
                    .message("Correct! Level already completed.")
                    .build();
        }

        return SubmitAnswerResponse.builder()
                .correct(false)
                .starsEarned(0)
                .xpEarned(0)
                .attempts(progress.getAttempts())
                .message("Wrong answer. Try again!")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LevelProgressDto> getProgressByUser(String userId) {
        return levelProgressRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LevelProgressDto getProgressByUserAndLevel(String userId, UUID levelId) {
        return levelProgressRepository.findByUserIdAndLevelId(userId, levelId)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Progress not found for user " + userId + " and level " + levelId));
    }

    private boolean isAnswerCorrect(String submitted, String acceptedAnswersJson) {
        if (submitted == null || acceptedAnswersJson == null) return false;
        String trimmed = submitted.trim();
        try {
            List<String> accepted = objectMapper.readValue(acceptedAnswersJson, new TypeReference<>() {});
            return accepted.stream().anyMatch(a -> a.trim().equalsIgnoreCase(trimmed));
        } catch (Exception e) {
            log.error("Failed to parse acceptedAnswers JSON: {}", acceptedAnswersJson, e);
            return false;
        }
    }

    private int calculateStars(int attempts) {
        if (attempts == 1) return 3;
        if (attempts <= 3) return 2;
        return 1;
    }

    private LevelProgressDto toDto(LevelProgress p) {
        return LevelProgressDto.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .levelId(p.getLevel().getId())
                .completed(p.isCompleted())
                .starsEarned(p.getStarsEarned())
                .attempts(p.getAttempts())
                .completedAt(p.getCompletedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
