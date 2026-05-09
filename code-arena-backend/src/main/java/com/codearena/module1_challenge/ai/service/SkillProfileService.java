package com.codearena.module1_challenge.ai.service;

import com.codearena.module1_challenge.ai.entity.UserSkillProfile;
import com.codearena.module1_challenge.ai.repository.UserSkillProfileRepository;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillProfileService {

    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserSkillProfileRepository skillProfileRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void rebuildAllUserProfiles() {
        log.info("Starting rebuild of all user AI skill profiles...");
        List<Submission> allSubs = submissionRepository.findAll();
        Map<String, List<Submission>> subsByUser = allSubs.stream()
                .filter(s -> s.getUserId() != null)
                .collect(Collectors.groupingBy(Submission::getUserId));

        for (Map.Entry<String, List<Submission>> entry : subsByUser.entrySet()) {
            try {
                buildSkillProfile(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("Failed to build profile for user {}: {}", entry.getKey(), e.getMessage());
            }
        }
        log.info("Finished AI skill profiles rebuild.");
    }

    @Transactional
    public void buildSkillProfile(String userId, List<Submission> userSubmissions) throws JsonProcessingException {
        if (userSubmissions == null) {
            userSubmissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        }

        UserSkillProfile profile = skillProfileRepository.findByUserId(userId)
                .orElse(UserSkillProfile.builder().userId(userId).build());

        profile.setTotalAttempted((int) userSubmissions.stream().map(s -> s.getChallenge().getId()).distinct().count());
        profile.setTotalSolved((int) userSubmissions.stream()
                .filter(s -> "ACCEPTED".equals(s.getStatus()))
                .map(s -> s.getChallenge().getId()).distinct().count());

        // Group subs by challenge
        Map<Long, List<Submission>> subsByChallenge = userSubmissions.stream()
                .collect(Collectors.groupingBy(s -> s.getChallenge().getId()));

        Map<String, Double> skillDict = new HashMap<>();
        Map<String, Integer> tagCounts = new HashMap<>();

        for (Map.Entry<Long, List<Submission>> entry : subsByChallenge.entrySet()) {
            List<Submission> subs = entry.getValue();
            boolean isSolved = subs.stream().anyMatch(s -> "ACCEPTED".equals(s.getStatus()));
            int attempts = subs.size();
            
            Challenge chal = subs.get(0).getChallenge();
            String[] tags = chal.getTags() != null ? chal.getTags().split(",") : new String[0];
            
            // Score for this challenge: solved=1.0, failing=0.0, penalized by attempts
            double challengeScore = isSolved ? (1.0 / Math.max(1, Math.log10(attempts + 1))) : 0.0;

            for (String tagRaw : tags) {
                String tag = tagRaw.trim().toUpperCase();
                if (tag.isEmpty()) continue;
                
                skillDict.put(tag, skillDict.getOrDefault(tag, 0.0) + challengeScore);
                tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
            }
        }

        // Normalize
        double totalScore = 0;
        for (String tag : skillDict.keySet()) {
            double normalized = skillDict.get(tag) / Math.max(1, tagCounts.get(tag));
            skillDict.put(tag, Math.min(1.0, normalized)); // cap at 1.0
            totalScore += skillDict.get(tag);
        }

        profile.setSkillVector(objectMapper.writeValueAsString(skillDict));
        profile.setOverallSkillRating((float) ((totalScore / Math.max(1, skillDict.size())) * 100.0));
        
        skillProfileRepository.save(profile);
    }
    
    public Map<String, Double> getUserSkillMap(String userId) {
        return skillProfileRepository.findByUserId(userId)
                .map(p -> {
                    try {
                        return objectMapper.readValue(p.getSkillVector(), new TypeReference<Map<String, Double>>() {});
                    } catch (Exception e) {
                        return new HashMap<String, Double>();
                    }
                }).orElse(new HashMap<>());
    }
}
