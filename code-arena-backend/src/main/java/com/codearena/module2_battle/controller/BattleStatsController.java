package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.BattleStatsResponse;
import com.codearena.module2_battle.service.BattleStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/battle/stats")
@RequiredArgsConstructor
public class BattleStatsController {

    private final BattleStatsService battleStatsService;

    @GetMapping("/")
    public ResponseEntity<BattleStatsResponse> getGlobalStats() {
        return ResponseEntity.ok(battleStatsService.getGlobalStats());
    }
}
