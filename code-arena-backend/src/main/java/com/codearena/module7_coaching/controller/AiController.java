package com.codearena.module7_coaching.controller;

import com.codearena.module7_coaching.dto.AiRequest;
import com.codearena.module7_coaching.dto.AiResponse;
import com.codearena.module7_coaching.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/coaching/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody AiRequest request) {
        log.info("AI request received - mode: {}, topic: {}, language: {}",
                request.getMode(), request.getTopic(), request.getLanguage());

        AiResponse response = aiService.processRequest(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "content", response.getContent(),
                            "mode", response.getMode()
                    )
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", response.getError() != null ? response.getError() : "AI generation failed"
            ));
        }
    }
}
