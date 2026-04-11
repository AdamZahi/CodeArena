package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.BattleFeedResponse;
import com.codearena.module2_battle.service.BattleFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/battle/feed")
@RequiredArgsConstructor
public class BattleFeedController {

    private final BattleFeedService battleFeedService;

    @GetMapping("/")
    public ResponseEntity<BattleFeedResponse> getFeed() {
        return ResponseEntity.ok(battleFeedService.getFeed());
    }
}
