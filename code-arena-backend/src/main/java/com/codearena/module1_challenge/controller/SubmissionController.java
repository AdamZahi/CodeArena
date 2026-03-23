package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.SubmissionDto;
import com.codearena.module1_challenge.dto.SubmitCodeRequest;
import com.codearena.module1_challenge.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<SubmissionDto> submitCode(@RequestBody SubmitCodeRequest request) {
        String mockUserId = "user-123"; // Using mock due to dev bypass
        return ResponseEntity.ok(submissionService.submitCode(request, mockUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> getSubmissionStatus(@PathVariable("id") Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionStatus(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubmissionDto>> getUserSubmissions(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(submissionService.getUserSubmissions(userId));
    }
}
