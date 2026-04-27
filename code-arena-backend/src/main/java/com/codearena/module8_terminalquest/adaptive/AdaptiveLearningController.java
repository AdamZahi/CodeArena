package com.codearena.module8_terminalquest.adaptive;

import lombok.RequiredArgsConstructor;
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
    public AdaptivePredictionResponse predict(@RequestBody Map<String, String> body) {
        String userId    = body.get("userId");
        UUID   missionId = UUID.fromString(body.get("missionId"));
        AdaptivePredictionRequest req = adaptiveService.buildPredictionRequest(userId, missionId);
        return adaptiveService.predict(req);
    }

    @PostMapping("/predict-raw")
    public AdaptivePredictionResponse predictRaw(@RequestBody AdaptivePredictionRequest req) {
        return adaptiveService.predict(req);
    }
}
