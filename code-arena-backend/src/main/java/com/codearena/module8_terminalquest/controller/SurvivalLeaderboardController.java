package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.SurvivalLeaderboardDto;
import com.codearena.module8_terminalquest.service.SurvivalLeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/terminal-quest/survival/leaderboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SurvivalLeaderboardController {

    private final SurvivalLeaderboardService survivalLeaderboardService;

    @GetMapping
    public ResponseEntity<List<SurvivalLeaderboardDto>> getLeaderboard() {
        return ResponseEntity.ok(survivalLeaderboardService.getLeaderboard());
    }

    @GetMapping("/me")
    public ResponseEntity<SurvivalLeaderboardDto> getMyRanking(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(survivalLeaderboardService.getUserRanking(jwt.getSubject()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SurvivalLeaderboardDto> getUserRanking(@PathVariable String userId) {
        return ResponseEntity.ok(survivalLeaderboardService.getUserRanking(userId));
    }
}
