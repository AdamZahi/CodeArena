package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.LevelProgressDto;
import com.codearena.module8_terminalquest.dto.SubmitAnswerRequest;
import com.codearena.module8_terminalquest.dto.SubmitAnswerResponse;
import com.codearena.module8_terminalquest.service.LevelProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/terminal-quest")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LevelProgressController {

    private final LevelProgressService levelProgressService;

    @PostMapping("/levels/{levelId}/submit")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable UUID levelId,
            @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        request.setUserId(jwt.getSubject());
        return ResponseEntity.ok(levelProgressService.submitAnswer(levelId, request));
    }

    @GetMapping("/progress/me")
    public ResponseEntity<List<LevelProgressDto>> getMyProgress(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(levelProgressService.getProgressByUser(jwt.getSubject()));
    }

    @GetMapping("/progress/level/{levelId}")
    public ResponseEntity<LevelProgressDto> getMyProgressByLevel(
            @PathVariable UUID levelId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(levelProgressService.getProgressByUserAndLevel(jwt.getSubject(), levelId));
    }

    @PostMapping("/missions/{missionId}/submit")
    public ResponseEntity<SubmitAnswerResponse> submitMissionAnswer(
            @PathVariable UUID missionId,
            @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        request.setUserId(jwt.getSubject());
        return ResponseEntity.ok(levelProgressService.submitMissionAnswer(missionId, request));
    }

    @GetMapping("/progress/mission/{missionId}")
    public ResponseEntity<LevelProgressDto> getMyProgressByMission(
            @PathVariable UUID missionId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(levelProgressService.getProgressByUserAndMission(jwt.getSubject(), missionId));
    }
}
