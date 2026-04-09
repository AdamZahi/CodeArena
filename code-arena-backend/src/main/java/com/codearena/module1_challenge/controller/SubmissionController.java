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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubmissionDto>> getUserSubmissions(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(submissionService.getUserSubmissions(userId));
    }
}
