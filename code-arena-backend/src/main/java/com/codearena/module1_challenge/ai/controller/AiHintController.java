package com.codearena.module1_challenge.ai.controller;

import com.codearena.module1_challenge.ai.dto.HintRequestDto;
import com.codearena.module1_challenge.ai.dto.HintResponseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiHintController {

    private final ChallengeRepository challengeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/challenge/{id}/hint")
    public ResponseEntity<HintResponseDto> generateAiHint(@PathVariable("id") Long id) {
        Challenge challenge = challengeRepository.findById(id).orElse(null);
        if (challenge == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Send ALL challenge context to the Python ML service
            HintRequestDto request = HintRequestDto.builder()
                    .title(challenge.getTitle())
                    .tags(challenge.getTags() != null ? challenge.getTags() : "")
                    .description(challenge.getDescription() != null ? challenge.getDescription() : "")
                    .difficulty(challenge.getDifficulty() != null ? challenge.getDifficulty() : "")
                    .build();

            String pythonAiUrl = "http://localhost:5000/predict-hint";
            ResponseEntity<HintResponseDto> response = restTemplate.postForEntity(pythonAiUrl, request, HintResponseDto.class);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Failed to fetch Hint from AI Microservice: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
