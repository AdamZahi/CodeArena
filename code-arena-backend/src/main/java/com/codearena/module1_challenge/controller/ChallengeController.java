package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.ChallengeDto;
import com.codearena.module1_challenge.dto.CreateChallengeRequest;
import com.codearena.module1_challenge.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<List<ChallengeDto>> getAllChallenges() {
        return ResponseEntity.ok(challengeService.getAllChallenges());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallengeDto> getChallengeById(@PathVariable UUID id) {
        return ResponseEntity.ok(challengeService.getChallengeById(id));
    }

    @PostMapping
    public ResponseEntity<ChallengeDto> createChallenge(@RequestBody CreateChallengeRequest request) {
        // Mock authorId since bypassing security for now
        String mockAuthorId = "admin-123";
        return ResponseEntity.ok(challengeService.createChallenge(request, mockAuthorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChallenge(@PathVariable UUID id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.ok().build();
    }
}
