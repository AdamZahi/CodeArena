package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.ChallengeVoteResponseDto;
import com.codearena.module1_challenge.service.ChallengeVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}")
public class ChallengeVoteController {

    private final ChallengeVoteService challengeVoteService;

    @PostMapping("/upvote")
    public ResponseEntity<ChallengeVoteResponseDto> upvote(@PathVariable("challengeId") Long challengeId, Authentication authentication) {
        return ResponseEntity.ok(challengeVoteService.upvote(challengeId, getUserId(authentication)));
    }

    @PostMapping("/downvote")
    public ResponseEntity<ChallengeVoteResponseDto> downvote(@PathVariable("challengeId") Long challengeId, Authentication authentication) {
        return ResponseEntity.ok(challengeVoteService.downvote(challengeId, getUserId(authentication)));
    }

    @GetMapping("/votes")
    public ResponseEntity<ChallengeVoteResponseDto> getVotes(@PathVariable("challengeId") Long challengeId, Authentication authentication) {
        String userId = null;
        try {
            userId = getUserId(authentication);
        } catch (Exception e) {
            // Unauthenticated user can still see vote counts
        }
        return ResponseEntity.ok(challengeVoteService.getVotes(challengeId, userId));
    }

    private String getUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken().getSubject();
        }
        throw new IllegalStateException("Authentication required");
    }
}
