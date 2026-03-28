package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.GlobalStatsDto;
import com.codearena.module8_terminalquest.dto.PlayerStatsDto;
import com.codearena.module8_terminalquest.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/terminal-quest/stats")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/{userId}")
    public ResponseEntity<PlayerStatsDto> getPlayerStats(@PathVariable String userId) {
        return ResponseEntity.ok(statsService.getPlayerStats(userId));
    }

    @GetMapping("/global")
    public ResponseEntity<GlobalStatsDto> getGlobalStats() {
        return ResponseEntity.ok(statsService.getGlobalStats());
    }
}
