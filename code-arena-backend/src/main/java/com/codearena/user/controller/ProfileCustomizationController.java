package com.codearena.user.controller;

import com.codearena.user.dto.EquipItemRequest;
import com.codearena.user.dto.UserUnlockDTO;
import com.codearena.user.service.CustomizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile/customization")
public class ProfileCustomizationController {

    private final CustomizationService customizationService;

    /** Get all items the current user has unlocked */
    @GetMapping("/unlocks")
    public ResponseEntity<List<UserUnlockDTO>> getMyUnlocks(@AuthenticationPrincipal Jwt jwt) {
        String auth0Id = jwt.getSubject();
        return ResponseEntity.ok(customizationService.getMyUnlocks(auth0Id));
    }

    /** Get unlocked items filtered by type (ICON, BORDER, TITLE, BADGE, BANNER) */
    @GetMapping("/unlocks/{type}")
    public ResponseEntity<List<UserUnlockDTO>> getMyUnlocksByType(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String type) {
        String auth0Id = jwt.getSubject();
        return ResponseEntity.ok(customizationService.getMyUnlocksByType(auth0Id, type));
    }

    /** Equip an item on the user's profile */
    @PostMapping("/equip")
    public ResponseEntity<Void> equipItem(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody EquipItemRequest request) {
        String auth0Id = jwt.getSubject();
        customizationService.equipItem(auth0Id, request);
        return ResponseEntity.ok().build();
    }

    /** Grant default items and check for new unlocks based on level/XP */
    @PostMapping("/sync")
    public ResponseEntity<Void> syncUnlocks(@AuthenticationPrincipal Jwt jwt) {
        String auth0Id = jwt.getSubject();
        customizationService.grantDefaultItems(auth0Id);
        customizationService.checkAndGrantUnlocks(auth0Id);
        return ResponseEntity.ok().build();
    }
}
