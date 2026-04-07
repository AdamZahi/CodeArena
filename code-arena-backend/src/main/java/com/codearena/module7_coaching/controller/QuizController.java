package com.codearena.module7_coaching.controller;

import com.codearena.module7_coaching.dto.*;
import com.codearena.module7_coaching.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /** Get all quizzes or a specific quiz by ID */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getQuizzes(@RequestParam(name = "id", required = false) UUID id) {
        if (id != null) {
            try {
                QuizDto quiz = quizService.getQuizById(id);
                return ResponseEntity.ok(Map.of("success", true, "data", quiz));
            } catch (Exception e) {
                log.error("Exception in getQuizById", e);
                return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
            }
        }
        List<QuizDto> quizzes = quizService.getAllQuizzes();
        return ResponseEntity.ok(Map.of("success", true, "data", quizzes));
    }

    /** Submit quiz answers and get results */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody SubmitQuizRequest request) {
        String userId = jwt.getSubject();
        QuizResultDto result = quizService.submitQuiz(userId, request);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    /** Get user's quiz history */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getUserHistory(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<QuizResultDto> history = quizService.getUserHistory(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", history));
    }

    /** Create a new quiz (admin/coach) */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createQuiz(@RequestBody QuizDto quizDto) {
        QuizDto created = quizService.createQuiz(quizDto);
        return ResponseEntity.ok(Map.of("success", true, "data", created));
    }

    /** Delete a quiz (admin) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteQuiz(@PathVariable(name = "id") UUID id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Quiz deleted"));
    }
}
