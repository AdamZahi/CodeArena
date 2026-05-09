package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.ChallengeVoteResponseDto;
import com.codearena.module1_challenge.service.ChallengeVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}")
@PreAuthorize("isAuthenticated()")
public class ChallengeVoteController {

    private final ChallengeVoteService challengeVoteService;

    @PostMapping("/upvote")
    public ResponseEntity<ChallengeVoteResponseDto> upvote(
            @PathVariable("challengeId") Long challengeId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(challengeVoteService.upvote(challengeId, jwt.getSubject()));
    }

    @PostMapping("/downvote")
    public ResponseEntity<ChallengeVoteResponseDto> downvote(
            @PathVariable("challengeId") Long challengeId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(challengeVoteService.downvote(challengeId, jwt.getSubject()));
    }

    @GetMapping("/votes")
    public ResponseEntity<ChallengeVoteResponseDto> getVotes(
            @PathVariable("challengeId") Long challengeId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(challengeVoteService.getVotes(challengeId, jwt.getSubject()));
    }
}
