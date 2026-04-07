package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.service.BattleResultsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle/results")
@RequiredArgsConstructor
public class BattleResultsController {

    private final BattleResultsService battleResultsService;

    @GetMapping("/{roomId}/scoreboard")
    public ResponseEntity<PostMatchSummaryResponse> getScoreboard(
            @PathVariable String roomId,
            JwtAuthenticationToken principal) {
        PostMatchSummaryResponse summary = battleResultsService.getPostMatchSummary(roomId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{roomId}/replay")
    public ResponseEntity<ReplayResponse> getReplay(
            @PathVariable String roomId,
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        ReplayResponse replay = battleResultsService.getReplay(roomId, userId);
        return ResponseEntity.ok(replay);
    }

    @GetMapping("/leaderboard/season")
    public ResponseEntity<SeasonLeaderboardResponse> getSeasonLeaderboard(
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        SeasonLeaderboardResponse leaderboard = battleResultsService.getSeasonLeaderboard(userId);
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/leaderboard/season/{userId}")
    public ResponseEntity<SeasonLeaderboardEntryResponse> getPlayerSeasonRank(
            @PathVariable String userId,
            JwtAuthenticationToken principal) {
        SeasonLeaderboardEntryResponse entry = battleResultsService.getPlayerSeasonRank(userId);
        return ResponseEntity.ok(entry);
    }
}
