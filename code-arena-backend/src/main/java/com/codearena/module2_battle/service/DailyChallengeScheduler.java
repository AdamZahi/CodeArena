package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.entity.DailyChallenge;
import com.codearena.module2_battle.repository.DailyChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a new Daily Challenge every day at UTC midnight.
 * Selects 1 EASY + 1 MEDIUM + 1 HARD challenge, preferring those not used in the last 14 days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyChallengeScheduler {

    private final DailyChallengeRepository dailyChallengeRepository;
    private final ChallengeRepository challengeRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void generateDailyChallenge() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Idempotency guard: skip if today's challenge already exists
        if (dailyChallengeRepository.findByChallengeDate(today).isPresent()) {
            log.warn("Daily challenge for {} already exists — skipping generation", today);
            return;
        }

        // Collect challenge IDs used in the last 14 days for recency filtering
        LocalDate since = today.minusDays(14);
        Set<String> recentChallengeIds = dailyChallengeRepository.findRecentSince(since).stream()
                .flatMap(dc -> dc.getChallengeIds().stream())
                .collect(Collectors.toSet());

        List<String> selectedIds = new ArrayList<>();

        // Select one challenge per difficulty tier: EASY, MEDIUM, HARD
        for (String difficulty : List.of("EASY", "MEDIUM", "HARD")) {
            String selected = selectChallenge(difficulty, recentChallengeIds, selectedIds);
            if (selected != null) {
                selectedIds.add(selected);
            }
        }

        // If any difficulty tier had no challenges, fill from any available difficulty
        if (selectedIds.size() < 3) {
            log.warn("Could not find challenges for all difficulty tiers — filling from available pool");
            List<Challenge> allChallenges = challengeRepository.findAll();
            Collections.shuffle(allChallenges);
            for (Challenge c : allChallenges) {
                if (selectedIds.size() >= 3) break;
                String cid = c.getId().toString();
                if (!selectedIds.contains(cid)) {
                    selectedIds.add(cid);
                }
            }
        }

        if (selectedIds.isEmpty()) {
            log.error("No challenges available at all — cannot generate daily challenge for {}", today);
            return;
        }

        DailyChallenge dailyChallenge = DailyChallenge.builder()
                .challengeDate(today)
                .challengeIds(selectedIds)
                .build();
        dailyChallengeRepository.save(dailyChallenge);

        log.info("Daily challenge generated for {}: challengeIds={}", today, selectedIds);
    }

    /**
     * Selects a challenge for the given difficulty, preferring those not used recently.
     * Returns the challenge ID or null if no challenges exist for this difficulty.
     */
    private String selectChallenge(String difficulty, Set<String> recentChallengeIds, List<String> alreadySelected) {
        List<Challenge> pool = challengeRepository.findByDifficulty(difficulty);
        if (pool.isEmpty()) {
            log.warn("No challenges found with difficulty={}", difficulty);
            return null;
        }

        // Prefer challenges not used in the last 14 days and not already selected
        List<Challenge> fresh = pool.stream()
                .filter(c -> !recentChallengeIds.contains(c.getId().toString()))
                .filter(c -> !alreadySelected.contains(c.getId().toString()))
                .collect(Collectors.toList());

        if (!fresh.isEmpty()) {
            Collections.shuffle(fresh);
            return fresh.get(0).getId().toString();
        }

        // Allow repeats if not enough unique challenges exist
        List<Challenge> available = pool.stream()
                .filter(c -> !alreadySelected.contains(c.getId().toString()))
                .collect(Collectors.toList());

        if (!available.isEmpty()) {
            Collections.shuffle(available);
            return available.get(0).getId().toString();
        }

        return null;
    }
}
