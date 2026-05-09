package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.DailyLeaderboardResponse;
import com.codearena.module2_battle.dto.LeaderboardPageRequest;
import com.codearena.module2_battle.dto.LeaderboardPageResponse;
import com.codearena.module2_battle.dto.XpLeaderboardPageResponse;
import com.codearena.module2_battle.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.ok(leaderboardService.getSeasonLeaderboardPage(userId, request));
    }

    @GetMapping("/xp")
    public ResponseEntity<XpLeaderboardPageResponse> getXpLeaderboard(
            @ModelAttribute LeaderboardPageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.ok(leaderboardService.getXpLeaderboard(userId, request));
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyLeaderboardResponse> getTodaysDailyLeaderboard(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.ok(leaderboardService.getDailyLeaderboard(LocalDate.now(), userId));
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<DailyLeaderboardResponse> getDailyLeaderboard(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.ok(leaderboardService.getDailyLeaderboard(date, userId));
    }
}
