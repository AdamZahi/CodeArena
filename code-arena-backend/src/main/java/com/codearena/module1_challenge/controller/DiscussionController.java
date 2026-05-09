package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.CommentRequestDto;
import com.codearena.module1_challenge.dto.CommentResponseDto;
import com.codearena.module1_challenge.service.DiscussionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges")
@PreAuthorize("isAuthenticated()")
public class DiscussionController {

    private final DiscussionService discussionService;

    @GetMapping("/{challengeId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable("challengeId") Long challengeId) {
        return ResponseEntity.ok(discussionService.getCommentsByChallenge(challengeId));
    }

    @PostMapping("/{challengeId}/comments")
    public ResponseEntity<CommentResponseDto> addComment(
            @PathVariable("challengeId") Long challengeId,
            @RequestBody CommentRequestDto request,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        
        // Prioritize the name from the request (if provided by UI), 
        // otherwise try to resolve from JWT
        String userName = request.getUserName();
        if (userName == null || userName.isBlank() || userName.startsWith("auth0|")) {
            userName = getUserName(authentication);
        }
        
        log.info("FINAL RESOLVED NAME FOR COMMENT: {}", userName);
        return ResponseEntity.ok(discussionService.createComment(challengeId, request, userId, userName));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable("commentId") Long commentId, Authentication authentication) {
        discussionService.deleteComment(commentId, getUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private String getUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken().getSubject();
        }
        throw new IllegalStateException("Authentication required");
    }

    private String getUserName(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            log.info("Resolving username. All Claims: {}", jwt.getClaims());

            // 1. Try common human-readable claims
            String nickname          = jwt.getClaimAsString("nickname");
            String name              = jwt.getClaimAsString("name");
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            String givenName         = jwt.getClaimAsString("given_name");
            String familyName        = jwt.getClaimAsString("family_name");
            String email             = jwt.getClaimAsString("email");

            // Priority Logic: Real human names / nicknames first
            String finalName = "User";

            if (nickname != null && !nickname.isBlank() && !nickname.contains("|")) {
                finalName = nickname;
            } else if (name != null && !name.isBlank() && !name.contains("|")) {
                finalName = name;
            } else if (givenName != null && familyName != null) {
                finalName = givenName + " " + familyName;
            } else if (preferredUsername != null && !preferredUsername.isBlank()) {
                finalName = preferredUsername;
            } else if (email != null && email.contains("@")) {
                finalName = email.substring(0, email.indexOf("@"));
            } else {
                finalName = jwt.getSubject(); // Fallback to ID
            }

            log.info("RESOLVED COMMENT USERNAME: {} (Mode: {})", finalName, (nickname != null ? "NICKNAME" : "FALLBACK"));
            return finalName;
        }
        return "User";
    }
}
