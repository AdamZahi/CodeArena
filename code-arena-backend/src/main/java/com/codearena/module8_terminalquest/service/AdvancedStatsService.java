package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.DifficultyStatDto;
import com.codearena.module8_terminalquest.dto.LeaderboardEntryDto;
import com.codearena.module8_terminalquest.dto.OverviewDto;
import com.codearena.module8_terminalquest.dto.PlayerSummaryDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdvancedStatsService {
    Page<LeaderboardEntryDto> getLeaderboard(int page, int size);
    List<PlayerSummaryDto> searchPlayers(String query);
    List<DifficultyStatDto> getDifficultyStats();
    OverviewDto getOverview();
}
