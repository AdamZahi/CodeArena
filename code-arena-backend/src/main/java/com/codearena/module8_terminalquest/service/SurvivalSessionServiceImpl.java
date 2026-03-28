package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.*;
import com.codearena.module8_terminalquest.entity.StoryLevel;
import com.codearena.module8_terminalquest.entity.SurvivalLeaderboard;
import com.codearena.module8_terminalquest.entity.SurvivalSession;
import com.codearena.module8_terminalquest.repository.StoryLevelRepository;
import com.codearena.module8_terminalquest.repository.SurvivalLeaderboardRepository;
import com.codearena.module8_terminalquest.repository.SurvivalSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurvivalSessionServiceImpl implements SurvivalSessionService {

    private static final int INITIAL_LIVES = 3;
    private static final int CORRECT_ANSWERS_PER_WAVE = 3;

    private final SurvivalSessionRepository survivalSessionRepository;
    private final SurvivalLeaderboardRepository survivalLeaderboardRepository;
    private final StoryLevelRepository storyLevelRepository;
    private final StoryLevelServiceImpl storyLevelService;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    @Transactional
    public SurvivalSessionDto startSession(StartSurvivalRequest request) {
        SurvivalSession session = SurvivalSession.builder()
                .userId(request.getUserId())
                .waveReached(1)
                .score(0)
                .livesRemaining(INITIAL_LIVES)
                .startedAt(Instant.now().toString())
                .build();
        return toDto(survivalSessionRepository.save(session));
    }

    @Override
    @Transactional
    public SurvivalAnswerResponse submitAnswer(UUID sessionId, SurvivalAnswerRequest request) {
        SurvivalSession session = survivalSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (session.getLivesRemaining() <= 0) {
            return SurvivalAnswerResponse.builder()
                    .correct(false)
                    .livesRemaining(0)
                    .waveReached(session.getWaveReached())
                    .score(session.getScore())
                    .gameOver(true)
                    .message("Game over! Session already ended.")
                    .build();
        }

        StoryLevel level = storyLevelRepository.findById(request.getLevelId())
                .orElseThrow(() -> new RuntimeException("Level not found: " + request.getLevelId()));

        boolean correct = isAnswerCorrect(request.getAnswer(), level.getAcceptedAnswers());

        if (!correct) {
            session.setLivesRemaining(session.getLivesRemaining() - 1);
            boolean gameOver = session.getLivesRemaining() <= 0;
            if (gameOver) {
                session.setEndedAt(Instant.now().toString());
                updateLeaderboard(session.getUserId(), session.getWaveReached(), session.getScore());
            }
            survivalSessionRepository.save(session);
            return SurvivalAnswerResponse.builder()
                    .correct(false)
                    .livesRemaining(session.getLivesRemaining())
                    .waveReached(session.getWaveReached())
                    .score(session.getScore())
                    .gameOver(gameOver)
                    .message(gameOver ? "Game over! You reached wave " + session.getWaveReached() + "." : "Wrong answer! " + session.getLivesRemaining() + " lives remaining.")
                    .build();
        }

        // Correct answer: add score and potentially advance wave
        int pointsEarned = session.getWaveReached() * 10;
        session.setScore(session.getScore() + pointsEarned);

        // Wave advances every CORRECT_ANSWERS_PER_WAVE correct answers (tracked via score milestones)
        int correctAnswers = session.getScore() / (session.getWaveReached() * 10);
        if (correctAnswers >= CORRECT_ANSWERS_PER_WAVE) {
            session.setWaveReached(session.getWaveReached() + 1);
        }

        survivalSessionRepository.save(session);

        StoryLevelDto nextChallenge = pickNextChallenge(session.getWaveReached());

        return SurvivalAnswerResponse.builder()
                .correct(true)
                .livesRemaining(session.getLivesRemaining())
                .waveReached(session.getWaveReached())
                .score(session.getScore())
                .gameOver(false)
                .message("Correct! +" + pointsEarned + " points. Wave " + session.getWaveReached() + ".")
                .nextChallenge(nextChallenge)
                .build();
    }

    @Override
    @Transactional
    public SurvivalSessionDto endSession(UUID sessionId) {
        SurvivalSession session = survivalSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        if (session.getEndedAt() != null) {
            return toDto(session);
        }
        session.setEndedAt(Instant.now().toString());
        updateLeaderboard(session.getUserId(), session.getWaveReached(), session.getScore());
        return toDto(survivalSessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurvivalSessionDto> getSessionsByUser(String userId) {
        return survivalSessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SurvivalSessionDto getSessionById(UUID id) {
        return toDto(survivalSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id)));
    }

    private void updateLeaderboard(String userId, int wave, int score) {
        SurvivalLeaderboard entry = survivalLeaderboardRepository.findByUserId(userId)
                .orElseGet(() -> SurvivalLeaderboard.builder().userId(userId).bestWave(0).bestScore(0).build());
        if (wave > entry.getBestWave() || (wave == entry.getBestWave() && score > entry.getBestScore())) {
            entry.setBestWave(wave);
            entry.setBestScore(score);
            survivalLeaderboardRepository.save(entry);
        }
    }

    private StoryLevelDto pickNextChallenge(int wave) {
        String difficulty = wave <= 3 ? "EASY" : wave <= 6 ? "MEDIUM" : "HARD";
        List<StoryLevel> levels = storyLevelRepository.findByDifficulty(difficulty);
        if (levels.isEmpty()) {
            levels = storyLevelRepository.findAll();
        }
        if (levels.isEmpty()) return null;
        StoryLevel picked = levels.get(random.nextInt(levels.size()));
        return storyLevelService.toDto(picked);
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

    private SurvivalSessionDto toDto(SurvivalSession s) {
        return SurvivalSessionDto.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .waveReached(s.getWaveReached())
                .score(s.getScore())
                .livesRemaining(s.getLivesRemaining())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
