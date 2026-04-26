package com.codearena.module2_battle.admin.analytics;

import com.codearena.module2_battle.admin.analytics.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/battles/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BattleAnalyticsController {

    private final BattleAnalyticsService service;

    @GetMapping("/summary")
    public ResponseEntity<BattleSummaryDTO> getSummary(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(service.getSummary(refresh));
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<BattleTimelineDTO>> getTimeline(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        if (to == null) to = LocalDateTime.now();
        if (from == null) from = to.minusDays(30);
        return ResponseEntity.ok(service.getTimeline(from, to, refresh));
    }

    @GetMapping("/top-challenges")
    public ResponseEntity<List<TopChallengeDTO>> getTopChallenges(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(service.getTopChallenges(limit, refresh));
    }

    @GetMapping("/top-players")
    public ResponseEntity<List<TopPlayerDTO>> getTopPlayers(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(service.getTopPlayers(limit, refresh));
    }

    @GetMapping("/language-distribution")
    public ResponseEntity<List<LanguageDistributionDTO>> getLanguageDistribution(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        if (to == null) to = LocalDateTime.now();
        if (from == null) from = to.minusDays(30);
        return ResponseEntity.ok(service.getLanguageDistribution(from, to, refresh));
    }

    @GetMapping("/outcome-distribution")
    public ResponseEntity<OutcomeDistributionDTO> getOutcomeDistribution(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(service.getOutcomeDistribution(refresh));
    }

    @GetMapping("/avg-duration")
    public ResponseEntity<AvgDurationDTO> getAverageDuration(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(service.getAverageDuration(refresh));
    }
}
