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
    private static final String ANONYMOUS_USER_ID = "anonymous";

    @PostMapping("/upvote")
    public ResponseEntity<ChallengeVoteResponseDto> upvote(@PathVariable("challengeId") Long challengeId, Authentication authentication) {
        return ResponseEntity.ok(challengeVoteService.upvote(challengeId, resolveUserId(authentication)));
    }

    @PostMapping("/downvote")
    public ResponseEntity<ChallengeVoteResponseDto> downvote(@PathVariable("challengeId") Long challengeId, Authentication authentication) {
        return ResponseEntity.ok(challengeVoteService.downvote(challengeId, resolveUserId(authentication)));
    }

    @GetMapping("/votes")
    public ResponseEntity<ChallengeVoteResponseDto> getVotes(@PathVariable("challengeId") Long challengeId, Authentication authentication) {
        return ResponseEntity.ok(challengeVoteService.getVotes(challengeId, resolveUserId(authentication)));
    }

    private String resolveUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken().getSubject();
        }
        return ANONYMOUS_USER_ID;
    }
}
