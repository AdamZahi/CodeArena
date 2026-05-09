package com.codearena.module1_challenge.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTrainingScheduler {

    private final DifficultyPredictorService difficultyPredictorService;
    private final SkillProfileService skillProfileService;

    // Run every 6 hours
    @Scheduled(cron = "0 0 */6 * * *")
    public void runAiTrainingCycle() {
        log.info("--- STARTING SCHEDULED AI TRAINING CYCLE ---");
        try {
            difficultyPredictorService.recalculateAll();
            skillProfileService.rebuildAllUserProfiles();
            log.info("--- AI TRAINING CYCLE COMPLETED SUCCESSFULLY ---");
        } catch (Exception e) {
            log.error("--- AI TRAINING CYCLE FAILED: {} ---", e.getMessage());
        }
    }
}
