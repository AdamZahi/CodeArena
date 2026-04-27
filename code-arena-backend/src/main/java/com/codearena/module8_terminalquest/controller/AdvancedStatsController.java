package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.DifficultyStatDto;
import com.codearena.module8_terminalquest.dto.LeaderboardEntryDto;
import com.codearena.module8_terminalquest.dto.OverviewDto;
import com.codearena.module8_terminalquest.dto.PlayerSummaryDto;
import com.codearena.module8_terminalquest.service.AdvancedStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terminal-quest/advanced")
@RequiredArgsConstructor
public class AdvancedStatsController {

    private final AdvancedStatsService advancedStatsService;

    @GetMapping("/leaderboard")
    public ResponseEntity<Page<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(advancedStatsService.getLeaderboard(page, size));
    }

    @GetMapping("/players/search")
    public ResponseEntity<List<PlayerSummaryDto>> searchPlayers(
            @RequestParam(defaultValue = "") String query) {
        return ResponseEntity.ok(advancedStatsService.searchPlayers(query));
    }

    @GetMapping("/difficulty-stats")
    public ResponseEntity<List<DifficultyStatDto>> getDifficultyStats() {
        return ResponseEntity.ok(advancedStatsService.getDifficultyStats());
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewDto> getOverview() {
        return ResponseEntity.ok(advancedStatsService.getOverview());
    }
}
