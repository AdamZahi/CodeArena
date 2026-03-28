package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.ChapterCompletionStatsDto;
import com.codearena.module8_terminalquest.dto.GlobalStatsDto;
import com.codearena.module8_terminalquest.dto.PlayerStatsDto;
import com.codearena.module8_terminalquest.entity.LevelProgress;
import com.codearena.module8_terminalquest.entity.StoryChapter;
import com.codearena.module8_terminalquest.repository.LevelProgressRepository;
import com.codearena.module8_terminalquest.repository.StoryChapterRepository;
import com.codearena.module8_terminalquest.repository.SurvivalLeaderboardRepository;
import com.codearena.module8_terminalquest.repository.SurvivalSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final LevelProgressRepository levelProgressRepository;
    private final SurvivalSessionRepository survivalSessionRepository;
    private final SurvivalLeaderboardRepository survivalLeaderboardRepository;
    private final StoryChapterRepository storyChapterRepository;

    @Override
    @Transactional(readOnly = true)
    public PlayerStatsDto getPlayerStats(String userId) {
        List<LevelProgress> allProgress = levelProgressRepository.findByUserId(userId);

        int totalLevelsCompleted = (int) allProgress.stream().filter(LevelProgress::isCompleted).count();
        int totalStarsEarned = levelProgressRepository.sumStarsEarnedByUserId(userId);
        int totalAttempts = allProgress.stream().mapToInt(LevelProgress::getAttempts).sum();
        int totalSurvivalSessions = survivalSessionRepository.findByUserIdOrderByCreatedAtDesc(userId).size();

        int bestWave = 0;
        int bestScore = 0;
        var leaderboardEntry = survivalLeaderboardRepository.findByUserId(userId);
        if (leaderboardEntry.isPresent()) {
            bestWave = leaderboardEntry.get().getBestWave();
            bestScore = leaderboardEntry.get().getBestScore();
        }

        return PlayerStatsDto.builder()
                .userId(userId)
                .totalLevelsCompleted(totalLevelsCompleted)
                .totalStarsEarned(totalStarsEarned)
                .totalAttempts(totalAttempts)
                .bestWave(bestWave)
                .bestScore(bestScore)
                .totalSurvivalSessions(totalSurvivalSessions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GlobalStatsDto getGlobalStats() {
        // Distinct players across story and survival
        Set<String> allPlayerIds = new HashSet<>(levelProgressRepository.findDistinctUserIds());
        allPlayerIds.addAll(survivalSessionRepository.findDistinctUserIds());
        long totalActivePlayers = allPlayerIds.size();

        long totalSurvivalSessions = survivalSessionRepository.count();
        long totalStoryAttempts = levelProgressRepository.count();
        long totalStoryCompletions = levelProgressRepository.findAll().stream()
                .filter(LevelProgress::isCompleted).count();
        double overallCompletionRate = totalStoryAttempts == 0 ? 0.0
                : (double) totalStoryCompletions / totalStoryAttempts;

        List<StoryChapter> chapters = storyChapterRepository.findAllByOrderByOrderIndexAsc();
        List<ChapterCompletionStatsDto> chapterStats = chapters.stream()
                .map(chapter -> {
                    long attempts = levelProgressRepository.countByChapterId(chapter.getId());
                    long completions = levelProgressRepository.countCompletedByChapterId(chapter.getId());
                    double rate = attempts == 0 ? 0.0 : (double) completions / attempts;
                    return ChapterCompletionStatsDto.builder()
                            .chapterId(chapter.getId())
                            .chapterTitle(chapter.getTitle())
                            .totalLevels(chapter.getLevels().size())
                            .totalAttempts(attempts)
                            .totalCompletions(completions)
                            .completionRate(rate)
                            .build();
                })
                .collect(Collectors.toList());

        return GlobalStatsDto.builder()
                .totalActivePlayers(totalActivePlayers)
                .totalSurvivalSessions(totalSurvivalSessions)
                .totalStoryAttempts(totalStoryAttempts)
                .totalStoryCompletions(totalStoryCompletions)
                .overallCompletionRate(overallCompletionRate)
                .chapterStats(chapterStats)
                .build();
    }
}
