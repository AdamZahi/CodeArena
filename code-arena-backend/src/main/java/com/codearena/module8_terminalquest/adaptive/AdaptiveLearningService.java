package com.codearena.module8_terminalquest.adaptive;

import com.codearena.module8_terminalquest.entity.LevelProgress;
import com.codearena.module8_terminalquest.entity.StoryMission;
import com.codearena.module8_terminalquest.repository.LevelProgressRepository;
import com.codearena.module8_terminalquest.repository.StoryMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveLearningService {

    private static final String FLASK_URL = "http://localhost:5000/adaptive-predict";

    private static final AdaptivePredictionResponse DEFAULT_RESPONSE = AdaptivePredictionResponse.builder()
            .successProbability(0.5)
            .recommendedAction("MAINTAIN")
            .timerAdjustment(0)
            .showHint(false)
            .difficultyLabel("MEDIUM")
            .playerLevel("LEARNING")
            .build();

    private final RestTemplate restTemplate = new RestTemplate();
    private final LevelProgressRepository levelProgressRepository;
    private final StoryMissionRepository storyMissionRepository;

    @SuppressWarnings("unchecked")
    public AdaptivePredictionResponse predict(AdaptivePredictionRequest req) {
        try {
            Map<String, Object> flaskRequest = new HashMap<>();
            flaskRequest.put("success_rate",       req.getSuccessRate());
            flaskRequest.put("avg_attempts",        req.getAvgAttempts());
            flaskRequest.put("avg_response_time",   req.getAvgResponseTime());
            flaskRequest.put("command_category",    req.getCommandCategory());
            flaskRequest.put("difficulty",          req.getDifficulty());
            flaskRequest.put("streak",              req.getStreak());

            log.info("[adaptive] sending to Flask (snake_case): {}", flaskRequest);

            Map<String, Object> raw = restTemplate.postForObject(FLASK_URL, flaskRequest, Map.class);
            log.info("[adaptive] Flask raw response: {}", raw);

            if (raw == null) return DEFAULT_RESPONSE;

            return AdaptivePredictionResponse.builder()
                    .successProbability(((Number) raw.get("success_probability")).doubleValue())
                    .recommendedAction((String) raw.get("recommended_action"))
                    .timerAdjustment(((Number) raw.get("timer_adjustment")).intValue())
                    .showHint((Boolean) raw.get("show_hint"))
                    .difficultyLabel((String) raw.get("difficulty_label"))
                    .playerLevel((String) raw.get("player_level"))
                    .build();
        } catch (Exception e) {
            log.error("Flask adaptive API unavailable: {}", e.getMessage());
            return DEFAULT_RESPONSE;
        }
    }

    public AdaptivePredictionRequest buildPredictionRequest(String userId, UUID missionId) {
        List<LevelProgress> progresses = levelProgressRepository.findByUserId(userId);
        List<LevelProgress> completed  = progresses.stream().filter(LevelProgress::isCompleted).toList();

        boolean hasHistory = completed.size() >= 3;

        double successRate = hasHistory
                ? (double) completed.size() / progresses.size()
                : 0.5;

        double avgAttempts = hasHistory
                ? progresses.stream().mapToInt(LevelProgress::getAttempts).average().orElse(2.0)
                : 2.0;

        double avgResponseTime = hasHistory
                ? Math.min(avgAttempts * 8.0, 90.0)
                : 30.0;

        int difficultyInt = 1;
        int commandCategory = 0;

        StoryMission mission = storyMissionRepository.findById(missionId).orElse(null);
        if (mission != null) {
            difficultyInt = mapDifficulty(mission.getDifficulty());
            commandCategory = mapCategory(mission.getContext(), mission.getTask());
        }

        int streak = hasHistory ? computeStreak(progresses) : 0;

        log.info("[adaptive] built request for user={} mission={}: successRate={} avgAttempts={} avgResponseTime={} category={} difficulty={} streak={} (hasHistory={})",
                userId, missionId, successRate, avgAttempts, avgResponseTime, commandCategory, difficultyInt, streak, hasHistory);

        AdaptivePredictionRequest req = new AdaptivePredictionRequest();
        req.setSuccessRate(successRate);
        req.setAvgAttempts(avgAttempts);
        req.setAvgResponseTime(avgResponseTime);
        req.setCommandCategory(commandCategory);
        req.setDifficulty(difficultyInt);
        req.setStreak(streak);
        return req;
    }

    private int mapDifficulty(String difficulty) {
        if (difficulty == null) return 1;
        return switch (difficulty.toUpperCase()) {
            case "EASY"   -> 0;
            case "HARD"   -> 2;
            case "BOSS"   -> 3;
            default       -> 1; // MEDIUM
        };
    }

    private int mapCategory(String context, String task) {
        String combined = ((context == null ? "" : context) + " " + (task == null ? "" : task)).toLowerCase();
        if (combined.matches(".*\\b(log|cat|ls|pwd|mkdir|rm|cp|mv|find)\\b.*"))       return 0; // filesystem
        if (combined.matches(".*\\b(nginx|port|netstat|curl|wget|ssh|ip|ping)\\b.*")) return 1; // network
        if (combined.matches(".*\\b(kill|process|ps|top|htop|pid)\\b.*"))             return 2; // process
        if (combined.matches(".*\\b(iptables|user|auth|chmod|sudo|passwd)\\b.*"))     return 3; // security
        if (combined.matches(".*\\b(disk|df|du|mount|fdisk|lsblk)\\b.*"))             return 4; // disk
        if (combined.matches(".*\\b(systemctl|service|daemon|unit)\\b.*"))            return 5; // service
        return 0;
    }

    private int computeStreak(List<LevelProgress> progresses) {
        if (progresses == null || progresses.isEmpty()) return 0;
        List<LevelProgress> sorted = progresses.stream()
                .filter(p -> p.getCreatedAt() != null)
                .sorted(Comparator.comparing(LevelProgress::getCreatedAt).reversed())
                .toList();

        if (sorted.isEmpty()) return 0;
        boolean firstCompleted = sorted.get(0).isCompleted();
        int streak = 0;
        for (LevelProgress p : sorted) {
            if (p.isCompleted() == firstCompleted) {
                streak += firstCompleted ? 1 : -1;
            } else {
                break;
            }
            if (Math.abs(streak) >= 5) break;
        }
        return streak;
    }
}
