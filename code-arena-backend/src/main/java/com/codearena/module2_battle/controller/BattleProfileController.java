package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.BattleProfileResponse;
import com.codearena.module2_battle.service.BattleProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle/profile")
@RequiredArgsConstructor
public class BattleProfileController {

    private final BattleProfileService battleProfileService;

    @GetMapping("/me")
    public ResponseEntity<BattleProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleProfileService.getProfile(userId, userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BattleProfileResponse> getUserProfile(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        String requestingUserId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.ok(battleProfileService.getProfile(userId, requestingUserId));
    }
}
