package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.service.BattleResultsService;
import com.codearena.module2_battle.service.SharedResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/battle/results")
@RequiredArgsConstructor
public class BattleResultsController {

    private final BattleResultsService battleResultsService;
    private final SharedResultService sharedResultService;

    @GetMapping("/{roomId}/scoreboard")
    public ResponseEntity<PostMatchSummaryResponse> getScoreboard(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        PostMatchSummaryResponse summary = battleResultsService.getPostMatchSummary(roomId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{roomId}/replay")
    public ResponseEntity<ReplayResponse> getReplay(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        ReplayResponse replay = battleResultsService.getReplay(roomId, userId);
        return ResponseEntity.ok(replay);
    }

    /**
     * Post-match transparency view: per-challenge cross-player metrics plus
     * the accepted source code for each player. Only callable by participants
     * (player or spectator) of the room.
     */
    @GetMapping("/{roomId}/compare")
    public ResponseEntity<MatchComparisonResponse> getMatchComparison(
            @PathVariable String roomId,
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        return ResponseEntity.ok(battleResultsService.getMatchComparison(roomId, userId));
    }

    @GetMapping("/leaderboard/season")
    public ResponseEntity<SeasonLeaderboardResponse> getSeasonLeaderboard(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        SeasonLeaderboardResponse leaderboard = battleResultsService.getSeasonLeaderboard(userId);
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/leaderboard/season/{userId}")
    public ResponseEntity<SeasonLeaderboardEntryResponse> getPlayerSeasonRank(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        SeasonLeaderboardEntryResponse entry = battleResultsService.getPlayerSeasonRank(userId);
        return ResponseEntity.ok(entry);
    }

    // ── Feature 3: Shareable Result Card ──────────────────────

    @PostMapping("/{roomId}/share")
    public ResponseEntity<ShareUrlResponse> createShareToken(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(sharedResultService.createOrGetShareToken(roomId, userId));
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<SharedResultDTO> getSharedResult(@PathVariable String token) {
        try {
            return ResponseEntity.ok(sharedResultService.getSharedResult(token));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
