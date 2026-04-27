package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.SurvivalLeaderboardDto;
import com.codearena.module8_terminalquest.entity.SurvivalLeaderboard;
import com.codearena.module8_terminalquest.repository.SurvivalLeaderboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurvivalLeaderboardServiceImpl implements SurvivalLeaderboardService {

    private final SurvivalLeaderboardRepository survivalLeaderboardRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SurvivalLeaderboardDto> getLeaderboard() {
        return survivalLeaderboardRepository.findAllByOrderByBestWaveDescBestScoreDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SurvivalLeaderboardDto getUserRanking(String userId) {
        return survivalLeaderboardRepository.findByUserId(userId)
                .map(this::toDto)
                .orElse(SurvivalLeaderboardDto.builder()
                        .userId(userId)
                        .bestWave(0)
                        .bestScore(0)
                        .build());
    }

    private SurvivalLeaderboardDto toDto(SurvivalLeaderboard entry) {
        return SurvivalLeaderboardDto.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .bestWave(entry.getBestWave())
                .bestScore(entry.getBestScore())
                .build();
    }
}
