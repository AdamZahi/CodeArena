package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.LevelProgressDto;
import com.codearena.module8_terminalquest.dto.SubmitAnswerRequest;
import com.codearena.module8_terminalquest.dto.SubmitAnswerResponse;
import com.codearena.module8_terminalquest.service.LevelProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
            @RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.ok(levelProgressService.submitAnswer(levelId, request));
    }

    @GetMapping("/progress/{userId}")
    public ResponseEntity<List<LevelProgressDto>> getProgressByUser(@PathVariable String userId) {
        return ResponseEntity.ok(levelProgressService.getProgressByUser(userId));
    }

    @GetMapping("/progress/{userId}/level/{levelId}")
    public ResponseEntity<LevelProgressDto> getProgressByUserAndLevel(
            @PathVariable String userId,
            @PathVariable UUID levelId) {
        return ResponseEntity.ok(levelProgressService.getProgressByUserAndLevel(userId, levelId));
    }
}
