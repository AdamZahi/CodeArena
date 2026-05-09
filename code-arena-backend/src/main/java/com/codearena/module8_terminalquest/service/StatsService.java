package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.GlobalStatsDto;
import com.codearena.module8_terminalquest.dto.PlayerStatsDto;

public interface StatsService {
    PlayerStatsDto getPlayerStats(String userId);
    GlobalStatsDto getGlobalStats();
}
