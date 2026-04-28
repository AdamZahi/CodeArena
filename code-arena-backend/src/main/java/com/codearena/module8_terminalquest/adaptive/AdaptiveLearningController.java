package com.codearena.module8_terminalquest.adaptive;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/terminal-quest/adaptive")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdaptiveLearningController {

    private final AdaptiveLearningService adaptiveService;

    @PostMapping("/predict")
    public AdaptivePredictionResponse predict(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID missionId = UUID.fromString(body.get("missionId"));
        AdaptivePredictionRequest req = adaptiveService.buildPredictionRequest(jwt.getSubject(), missionId);
        return adaptiveService.predict(req);
    }

    @PostMapping("/predict-raw")
    public AdaptivePredictionResponse predictRaw(@RequestBody AdaptivePredictionRequest req) {
        return adaptiveService.predict(req);
    }
}
