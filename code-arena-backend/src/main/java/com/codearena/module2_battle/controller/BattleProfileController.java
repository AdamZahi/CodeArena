package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.BattleProfileResponse;
import com.codearena.module2_battle.service.BattleProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle/profile")
@RequiredArgsConstructor
public class BattleProfileController {

    private final BattleProfileService battleProfileService;

    @GetMapping("/me")
    public ResponseEntity<BattleProfileResponse> getMyProfile(
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        return ResponseEntity.ok(battleProfileService.getProfile(userId, userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BattleProfileResponse> getUserProfile(
            @PathVariable String userId,
            JwtAuthenticationToken principal) {
        String requestingUserId = principal != null ? principal.getToken().getSubject() : null;
        return ResponseEntity.ok(battleProfileService.getProfile(userId, requestingUserId));
    }
}
