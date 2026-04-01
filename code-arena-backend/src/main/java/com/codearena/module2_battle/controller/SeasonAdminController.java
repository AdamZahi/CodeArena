package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.CreateSeasonRequest;
import com.codearena.module2_battle.dto.SeasonResponse;
import com.codearena.module2_battle.dto.SeasonSummaryResponse;
import com.codearena.module2_battle.service.SeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/battle/seasons")
@RequiredArgsConstructor
public class SeasonAdminController {

    private final SeasonService seasonService;

    @PostMapping("/close")
    public ResponseEntity<SeasonSummaryResponse> closeActiveSeason(
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        return ResponseEntity.ok(seasonService.closeActiveSeason(userId));
    }

    @PostMapping("/")
    public ResponseEntity<SeasonResponse> createNextSeason(
            @RequestBody @Valid CreateSeasonRequest request,
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        return ResponseEntity.ok(seasonService.createNextSeason(userId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SeasonSummaryResponse>> getSeasonHistory() {
        return ResponseEntity.ok(seasonService.getSeasonHistory());
    }
}
