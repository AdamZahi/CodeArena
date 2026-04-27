package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.SurvivalLeaderboardDto;

import java.util.List;

public interface SurvivalLeaderboardService {
    List<SurvivalLeaderboardDto> getLeaderboard();
    SurvivalLeaderboardDto getUserRanking(String userId);
}
