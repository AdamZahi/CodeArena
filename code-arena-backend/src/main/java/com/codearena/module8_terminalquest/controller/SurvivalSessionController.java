package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.*;
import com.codearena.module8_terminalquest.service.SurvivalSessionService;
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
@RequestMapping("/api/terminal-quest/survival")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SurvivalSessionController {

    private final SurvivalSessionService survivalSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<SurvivalSessionDto> startSession(
            @RequestBody StartSurvivalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        request.setUserId(jwt.getSubject());
        return ResponseEntity.ok(survivalSessionService.startSession(request));
    }

    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<SurvivalSessionDto> endSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(survivalSessionService.endSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public ResponseEntity<SurvivalAnswerResponse> submitAnswer(
            @PathVariable UUID sessionId,
            @RequestBody SurvivalAnswerRequest request) {
        return ResponseEntity.ok(survivalSessionService.submitAnswer(sessionId, request));
    }

    @GetMapping("/sessions/me")
    public ResponseEntity<List<SurvivalSessionDto>> getMySessions(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(survivalSessionService.getSessionsByUser(jwt.getSubject()));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<SurvivalSessionDto> getSessionById(@PathVariable UUID id) {
        return ResponseEntity.ok(survivalSessionService.getSessionById(id));
    }
}
