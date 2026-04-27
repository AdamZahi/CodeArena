package com.codearena.module8_terminalquest.skill;

import com.codearena.module8_terminalquest.entity.LevelProgress;
import com.codearena.module8_terminalquest.entity.StoryMission;
import com.codearena.module8_terminalquest.repository.LevelProgressRepository;
import com.codearena.module8_terminalquest.repository.StoryMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillEngineService {

    private static final String FLASK_URL = "http://localhost:5000/skill-analyze";
    private static final List<String> CATEGORIES =
            List.of("filesystem", "network", "process", "security", "disk", "service");

    private final RestTemplate restTemplate = new RestTemplate();
    private final LevelProgressRepository levelProgressRepository;
    private final StoryMissionRepository storyMissionRepository;

    @SuppressWarnings("unchecked")
    public SkillAnalysisResponse analyzePlayer(String userId) {
        List<LevelProgress> progresses = levelProgressRepository.findByUserId(userId);
        List<StoryMission>  missions   = storyMissionRepository.findAll();

        Map<UUID, String> missionCategory = new HashMap<>();
        for (StoryMission m : missions) {
            missionCategory.put(m.getId(), detectCategory(m.getContext(), m.getTask()));
        }

        Map<String, List<LevelProgress>> byCategory = new HashMap<>();
        for (String cat : CATEGORIES) byCategory.put(cat, new ArrayList<>());

        for (LevelProgress lp : progresses) {
            UUID mId = lp.getMission() != null ? lp.getMission().getId() : null;
            String cat = mId != null ? missionCategory.getOrDefault(mId, "filesystem") : "filesystem";
            byCategory.get(cat).add(lp);
        }

        Map<String, Object> categoryStats = new HashMap<>();
        for (String cat : CATEGORIES) {
            List<LevelProgress> list = byCategory.get(cat);
            int completed    = (int) list.stream().filter(LevelProgress::isCompleted).count();
            int totalAttempts = list.stream().mapToInt(LevelProgress::getAttempts).sum();
            int totalStars    = list.stream().mapToInt(LevelProgress::getStarsEarned).sum();
            double avgTime    = list.isEmpty() ? 0.0 :
                    Math.min(list.stream().mapToInt(LevelProgress::getAttempts).average().orElse(1.0) * 8.0, 90.0);

            Map<String, Object> stat = new HashMap<>();
            stat.put("completed",      completed);
            stat.put("total_attempts", totalAttempts);
            stat.put("total_stars",    totalStars);
            stat.put("avg_time",       avgTime);
            categoryStats.put(cat, stat);
        }

        int totalCompleted = (int) progresses.stream().filter(LevelProgress::isCompleted).count();
        double avgAttempts = progresses.isEmpty() ? 1.0 :
                progresses.stream().mapToInt(LevelProgress::getAttempts).average().orElse(1.0);
        double avgStars = progresses.isEmpty() ? 0.0 :
                progresses.stream().mapToInt(LevelProgress::getStarsEarned).average().orElse(0.0);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id",                  userId);
        body.put("category_stats",           categoryStats);
        body.put("total_missions_completed", totalCompleted);
        body.put("avg_attempts",             avgAttempts);
        body.put("avg_stars",                avgStars);

        log.info("[skill] sending to Flask for userId={}: totalCompleted={}", userId, totalCompleted);

        try {
            Map<String, Object> responseMap = restTemplate.postForObject(FLASK_URL, body, Map.class);
            log.info("[skill] Raw Flask response: {}", responseMap);
            log.info("[skill] certification_readiness from Flask: {}",
                    responseMap != null ? responseMap.get("certification_readiness") : "null");
            if (responseMap == null) return defaultResponse();
            return mapResponse(responseMap);
        } catch (Exception e) {
            log.error("[skill] Flask unavailable: {}", e.getMessage());
            return defaultResponse();
        }
    }

    @SuppressWarnings("unchecked")
    private SkillAnalysisResponse mapResponse(Map<String, Object> responseMap) {
        // skill_profile → skillProfile (needs type conversion Number → Double)
        Map<String, Double> skillProfile = new LinkedHashMap<>();
        Map<String, Object> rawProfile = (Map<String, Object>) responseMap.get("skill_profile");
        if (rawProfile != null) {
            rawProfile.forEach((k, v) -> skillProfile.put(k, ((Number) v).doubleValue()));
        }

        // Pass certification_readiness directly — Angular template handles both snake_case keys
        Map<String, Object> certReadiness =
                (Map<String, Object>) responseMap.get("certification_readiness");
        log.info("[skill] certification_readiness keys: {}",
                certReadiness != null ? certReadiness.keySet() : "null");

        // Pass recommendations directly — Angular template reads suggested_missions
        List<Map<String, Object>> recommendations =
                (List<Map<String, Object>>) responseMap.get("recommendations");

        return SkillAnalysisResponse.builder()
                .skillProfile(skillProfile)
                .overallScore(responseMap.get("overall_score") != null
                        ? ((Number) responseMap.get("overall_score")).doubleValue() : 0.0)
                .predictedWeakness((String) responseMap.get("predicted_weakness"))
                .weaknessConfidence(responseMap.get("weakness_confidence") != null
                        ? ((Number) responseMap.get("weakness_confidence")).doubleValue() : 0.0)
                .certificationReadiness(certReadiness != null ? certReadiness : new HashMap<>())
                .recommendations(recommendations != null ? recommendations : new ArrayList<>())
                .playerTitle((String) responseMap.get("player_title"))
                .nextTitle((String) responseMap.get("next_title"))
                .progressToNextTitle(responseMap.get("progress_to_next_title") != null
                        ? ((Number) responseMap.get("progress_to_next_title")).doubleValue() : 0.0)
                .build();
    }

    private String detectCategory(String context, String task) {
        String text = ((context == null ? "" : context) + " " + (task == null ? "" : task)).toLowerCase();
        if (text.matches(".*\\b(log|cat|ls|pwd|find|directory|file|mkdir|rm|cp|mv)\\b.*"))
            return "filesystem";
        if (text.matches(".*\\b(nginx|port|netstat|lsof|http|network|curl|wget|ssh|ip|ping)\\b.*"))
            return "network";
        if (text.matches(".*\\b(process|kill|ps|top|htop|pid)\\b.*"))
            return "process";
        if (text.matches(".*\\b(iptables|auth|userdel|hacker|intrusion|block|security|chmod|sudo|passwd)\\b.*"))
            return "security";
        if (text.matches(".*\\b(disk|df|du|gzip|compress|storage|mount|fdisk|lsblk)\\b.*"))
            return "disk";
        if (text.matches(".*\\b(systemctl|service|restart|status|daemon|unit)\\b.*"))
            return "service";
        return "filesystem";
    }

    private SkillAnalysisResponse defaultResponse() {
        Map<String, Double> profile = new LinkedHashMap<>();
        CATEGORIES.forEach(c -> profile.put(c, 0.0));
        return SkillAnalysisResponse.builder()
                .skillProfile(profile)
                .overallScore(0.0)
                .predictedWeakness("unknown")
                .weaknessConfidence(0.0)
                .certificationReadiness(new HashMap<>())
                .recommendations(new ArrayList<>())
                .playerTitle("Trainee")
                .nextTitle("Junior Operator")
                .progressToNextTitle(0.0)
                .build();
    }
}
