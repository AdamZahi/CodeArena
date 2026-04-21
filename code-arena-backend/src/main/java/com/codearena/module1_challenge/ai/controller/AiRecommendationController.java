package com.codearena.module1_challenge.ai.controller;

import com.codearena.module1_challenge.ai.dto.ChallengeDifficultyDto;
import com.codearena.module1_challenge.ai.dto.RecommendationDto;
import com.codearena.module1_challenge.ai.dto.UserSkillProfileDto;
import com.codearena.module1_challenge.ai.entity.ChallengeDifficultyProfile;
import com.codearena.module1_challenge.ai.repository.ChallengeDifficultyProfileRepository;
import com.codearena.module1_challenge.ai.service.AiTrainingScheduler;
import com.codearena.module1_challenge.ai.service.RecommendationService;
import com.codearena.module1_challenge.ai.service.SkillProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final RecommendationService recommendationService;
    private final SkillProfileService skillProfileService;
    private final AiTrainingScheduler aiTrainingScheduler;
    private final ChallengeDifficultyProfileRepository difficultyRepository;

    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecommendationDto>> getRecommendations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "5") int count) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(recommendationService.getRecommendations(userId, count));
    }

    @GetMapping("/skill-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSkillProfileDto> getSkillProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Map<String, Double> skillMap = skillProfileService.getUserSkillMap(userId);
        
        // Find strongest and weakest
        String strongest = null;
        String weakest = null;
        double maxObj = -1.0;
        double minObj = 2.0;
        
        for (Map.Entry<String, Double> entry : skillMap.entrySet()) {
            if (entry.getValue() > maxObj) {
                maxObj = entry.getValue();
                strongest = entry.getKey();
            }
            if (entry.getValue() < minObj) {
                minObj = entry.getValue();
                weakest = entry.getKey();
            }
        }
        
        return ResponseEntity.ok(UserSkillProfileDto.builder()
                .userId(userId)
                .skillMap(skillMap)
                .strongestTag(strongest)
                .weakestTag(weakest)
                .build());
    }

    @GetMapping("/challenge/{id}/difficulty")
    public ResponseEntity<ChallengeDifficultyDto> getChallengeDifficulty(@PathVariable("id") Long id) {
        ChallengeDifficultyProfile profile = difficultyRepository.findByChallengeId(id).orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ChallengeDifficultyDto.builder()
                .challengeId(id)
                .aiDifficultyScore(profile.getAiDifficultyScore())
                .passRate(profile.getPassRate())
                .avgAttempts(profile.getAvgAttempts())
                .humanDifficulty(profile.getChallenge().getDifficulty())
                .build());
    }

    @PostMapping("/retrain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceRetrain() {
        log.info("Admin triggered manual AI retrain.");
        new Thread(aiTrainingScheduler::runAiTrainingCycle).start(); // Don't block HTTP thread
        return ResponseEntity.ok("AI retraining started in background.");
    }
}
