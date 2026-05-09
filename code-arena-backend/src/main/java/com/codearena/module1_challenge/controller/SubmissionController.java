package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.SubmissionDto;
import com.codearena.module1_challenge.dto.SubmitCodeRequest;
import com.codearena.module1_challenge.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<SubmissionDto> submitCode(
            @RequestBody SubmitCodeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(submissionService.submitCode(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> getSubmissionStatus(@PathVariable("id") Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionStatus(id));
    }

    @GetMapping("/me")
    public ResponseEntity<List<SubmissionDto>> getMySubmissions(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(submissionService.getUserSubmissions(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubmissionDto>> getUserSubmissions(
            @PathVariable("userId") String userId,
            @AuthenticationPrincipal Jwt jwt) {

        String requesterId = jwt.getSubject();
        boolean isAdmin = jwt.getClaimAsStringList("https://codearena.com/roles") != null
                && jwt.getClaimAsStringList("https://codearena.com/roles").contains("ADMIN");

        // Participant can only see their own submissions
        // Admin can see anyone's
        if (!isAdmin && !requesterId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(submissionService.getUserSubmissions(userId));
    }
}
