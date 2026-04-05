package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.DailyLeaderboardResponse;
import com.codearena.module2_battle.dto.LeaderboardPageRequest;
import com.codearena.module2_battle.dto.LeaderboardPageResponse;
import com.codearena.module2_battle.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/battle/leaderboard")
@RequiredArgsConstructor
public class BattleLeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/season")
    public ResponseEntity<LeaderboardPageResponse> getSeasonLeaderboard(
            @ModelAttribute LeaderboardPageRequest request,
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        return ResponseEntity.ok(leaderboardService.getSeasonLeaderboardPage(userId, request));
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyLeaderboardResponse> getTodaysDailyLeaderboard(
            JwtAuthenticationToken principal) {
        String userId = principal != null ? principal.getToken().getSubject() : null;
        return ResponseEntity.ok(leaderboardService.getDailyLeaderboard(LocalDate.now(), userId));
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<DailyLeaderboardResponse> getDailyLeaderboard(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            JwtAuthenticationToken principal) {
        String userId = principal != null ? principal.getToken().getSubject() : null;
        return ResponseEntity.ok(leaderboardService.getDailyLeaderboard(date, userId));
    }
}
