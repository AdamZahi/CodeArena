package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.MatchHistoryPageRequest;
import com.codearena.module2_battle.dto.MatchHistoryPageResponse;
import com.codearena.module2_battle.service.MatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle/history")
@RequiredArgsConstructor
public class BattleHistoryController {

    private final MatchHistoryService matchHistoryService;

    @GetMapping("/me")
    public ResponseEntity<MatchHistoryPageResponse> getMyMatchHistory(
            @ModelAttribute MatchHistoryPageRequest request,
            @AuthenticationPrincipal JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        return ResponseEntity.ok(matchHistoryService.getMatchHistory(userId, userId, request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<MatchHistoryPageResponse> getUserMatchHistory(
            @PathVariable String userId,
            @ModelAttribute MatchHistoryPageRequest request,
            @AuthenticationPrincipal JwtAuthenticationToken principal) {
        String requestingUserId = principal != null ? principal.getToken().getSubject() : null;
        return ResponseEntity.ok(matchHistoryService.getMatchHistory(userId, requestingUserId, request));
    }
}
