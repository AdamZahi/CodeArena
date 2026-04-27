package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.DifficultyStatDto;
import com.codearena.module8_terminalquest.dto.LeaderboardEntryDto;
import com.codearena.module8_terminalquest.dto.OverviewDto;
import com.codearena.module8_terminalquest.dto.PlayerSummaryDto;
import com.codearena.module8_terminalquest.entity.SurvivalLeaderboard;
import com.codearena.module8_terminalquest.repository.LevelProgressRepository;
import com.codearena.module8_terminalquest.repository.StoryLevelRepository;
import com.codearena.module8_terminalquest.repository.SurvivalLeaderboardRepository;
import com.codearena.module8_terminalquest.repository.SurvivalSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedStatsServiceImpl implements AdvancedStatsService {

    private final LevelProgressRepository levelProgressRepository;
    private final SurvivalLeaderboardRepository survivalLeaderboardRepository;
    private final SurvivalSessionRepository survivalSessionRepository;
    private final StoryLevelRepository storyLevelRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LeaderboardEntryDto> getLeaderboard(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<SurvivalLeaderboard> raw = survivalLeaderboardRepository
                .findAllByOrderByBestScoreDesc(pageable);

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int rank = page * size + 1;
        for (SurvivalLeaderboard lb : raw.getContent()) {
            entries.add(LeaderboardEntryDto.builder()
                    .rank(rank++)
                    .userId(lb.getUserId())
                    .bestWave(lb.getBestWave())
                    .bestScore(lb.getBestScore())
                    .build());
        }
        return new PageImpl<>(entries, pageable, raw.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerSummaryDto> searchPlayers(String query) {
        List<String> userIds = levelProgressRepository.searchUserIds(query);
        List<PlayerSummaryDto> result = new ArrayList<>();
        for (String userId : userIds) {
            long total = levelProgressRepository.findByUserId(userId).size();
            long completed = levelProgressRepository.findByUserIdAndCompleted(userId, true).size();
            int stars = levelProgressRepository.sumStarsEarnedByUserId(userId);
            double rate = total > 0 ? (double) completed / total * 100 : 0.0;
            result.add(PlayerSummaryDto.builder()
                    .userId(userId)
                    .totalAttempts(total)
                    .completedMissions(completed)
                    .totalStars(stars)
                    .completionRate(Math.round(rate * 10.0) / 10.0)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DifficultyStatDto> getDifficultyStats() {
        return buildDifficultyStats();
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewDto getOverview() {
        List<String> allUsers = levelProgressRepository.findDistinctUserIds();
        long totalAttempts = levelProgressRepository.count();

        long completions = 0;
        for (String uid : allUsers) {
            completions += levelProgressRepository.findByUserIdAndCompleted(uid, true).size();
        }

        long survivalSessions = survivalSessionRepository.count();
        double rate = totalAttempts > 0 ? (double) completions / totalAttempts * 100 : 0.0;

        return OverviewDto.builder()
                .totalPlayers(allUsers.size())
                .totalMissionAttempts(totalAttempts)
                .totalMissionCompletions(completions)
                .totalSurvivalSessions(survivalSessions)
                .overallCompletionRate(Math.round(rate * 10.0) / 10.0)
                .difficultyBreakdown(buildDifficultyStats())
                .build();
    }

    private List<DifficultyStatDto> buildDifficultyStats() {
        List<String> difficulties = List.of("EASY", "MEDIUM", "HARD");
        List<DifficultyStatDto> result = new ArrayList<>();
        for (String diff : difficulties) {
            long total = storyLevelRepository.countAttemptsByDifficulty(diff);
            long done  = storyLevelRepository.countCompletionsByDifficulty(diff);
            double rate = total > 0 ? (double) done / total * 100 : 0.0;
            result.add(DifficultyStatDto.builder()
                    .difficulty(diff)
                    .totalAttempts(total)
                    .completions(done)
                    .completionRate(Math.round(rate * 10.0) / 10.0)
                    .build());
        }
        return result;
    }
}
