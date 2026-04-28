package com.codearena.module7_coaching.controller;

import com.codearena.module7_coaching.dto.CodeSubmissionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Controller that proxies requests to the Python AI Memory Tracker microservice.
 * Handles code analysis, weakness profile retrieval, and mistake history.
 */
@Slf4j
@RestController
@RequestMapping("/api/coaching/ai-memory")
public class AiMemoryController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.memory.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    /**
     * Submit code for analysis — detects errors and stores in student memory.
     */
    @PostMapping("/analyze")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> analyzeCode(@Valid @RequestBody CodeSubmissionRequest request) {
        log.info("AI Memory: analyzing code for student={}, lang={}", request.getStudentId(), request.getLanguage());
        try {
            Map<String, Object> body = Map.of(
                "student_id", request.getStudentId(),
                "language", request.getLanguage(),
                "code", request.getCode()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiServiceUrl + "/api/ai-memory/analyze", entity, Map.class
            );

            return ResponseEntity.ok(Map.of("success", true, "data", response.getBody()));
        } catch (Exception e) {
            log.error("AI Memory analysis failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "AI analysis service unavailable: " + e.getMessage()
            ));
        }
    }

    /**
     * Get weakness profile for a student.
     */
    @GetMapping("/profile/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String studentId) {
        log.info("AI Memory: fetching profile for student={}", studentId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                aiServiceUrl + "/api/ai-memory/profile/" + studentId, Map.class
            );
            return ResponseEntity.ok(Map.of("success", true, "data", response.getBody()));
        } catch (Exception e) {
            log.error("AI Memory profile fetch failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "AI memory service unavailable: " + e.getMessage()
            ));
        }
    }

    /**
     * Get recent mistakes for a student.
     */
    @GetMapping("/mistakes/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMistakes(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("AI Memory: fetching mistakes for student={}", studentId);
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                aiServiceUrl + "/api/ai-memory/mistakes/" + studentId + "?limit=" + limit, Object.class
            );
            return ResponseEntity.ok(Map.of("success", true, "data", response.getBody()));
        } catch (Exception e) {
            log.error("AI Memory mistakes fetch failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "AI memory service unavailable: " + e.getMessage()
            ));
        }
    }
}
