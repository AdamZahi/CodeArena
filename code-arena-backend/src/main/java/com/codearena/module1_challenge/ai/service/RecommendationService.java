package com.codearena.module1_challenge.ai.service;

import com.codearena.module1_challenge.ai.dto.RecommendationDto;
import com.codearena.module1_challenge.ai.entity.ChallengeDifficultyProfile;
import com.codearena.module1_challenge.ai.entity.UserSkillProfile;
import com.codearena.module1_challenge.ai.repository.ChallengeDifficultyProfileRepository;
import com.codearena.module1_challenge.ai.repository.UserSkillProfileRepository;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ChallengeRepository challengeRepository;
    private final SubmissionRepository submissionRepository;
    private final UserSkillProfileRepository userSkillProfileRepository;
    private final ChallengeDifficultyProfileRepository difficultyProfileRepository;
    private final SkillProfileService skillProfileService;

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendations(String userId, int count) {
        log.info("Generating AI recommendations for user {}", userId);
        
        Map<String, Double> userSkills = skillProfileService.getUserSkillMap(userId);
        UserSkillProfile userProfile = userSkillProfileRepository.findByUserId(userId).orElse(null);
        
        float overallSkill = userProfile != null ? userProfile.getOverallSkillRating() : 50.0f;
        
        // Get all challenges
        List<Challenge> allChallenges = challengeRepository.findAll();
        
        // Get user's solved challenges
        Set<Long> solvedIds = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .filter(s -> "ACCEPTED".equals(s.getStatus()))
                .map(s -> s.getChallenge().getId())
                .collect(Collectors.toSet());

        List<RecommendationDto> recommendations = new ArrayList<>();
        
        for (Challenge challenge : allChallenges) {
            if (solvedIds.contains(challenge.getId())) {
                continue; // Skip already solved
            }
            
            ChallengeDifficultyProfile diffProfile = difficultyProfileRepository.findByChallengeId(challenge.getId())
                    .orElse(null);
                    
            float aiDiff = diffProfile != null ? diffProfile.getAiDifficultyScore() : 50.0f;
            
            // Calculate fit
            // Ideal challenge is slightly harder than current overall skill (Zone of Proximal Development)
            float diffDelta = aiDiff - overallSkill;
            float matchScore = 100.0f;
            
            if (diffDelta < -15) {
                // Too easy
                matchScore -= (Math.abs(diffDelta) * 2);
            } else if (diffDelta > 25) {
                // Too hard
                matchScore -= (diffDelta * 1.5f);
            } else {
                // Sweet spot
                matchScore += 10.0f; 
            }
            
            String reason = "Perfect match for your skill level";
            
            // Check tags against weak areas
            String[] tags = challenge.getTags() != null ? challenge.getTags().split(",") : new String[0];
            String weakTag = null;
            for (String tagRaw : tags) {
                String tag = tagRaw.trim().toUpperCase();
                double proficiency = userSkills.getOrDefault(tag, -1.0);
                if (proficiency >= 0 && proficiency < 0.4) {
                    weakTag = tag;
                    matchScore += 15.0f; // Boost challenges that train weak areas
                    break;
                }
            }
            
            if (weakTag != null) {
                reason = "Strengthens your weak area: " + weakTag;
            } else if (diffDelta > 5 && diffDelta <= 25) {
                reason = "Pushes your limits (Challenging)";
            } else if (diffDelta < -5) {
                reason = "Good for a quick warmup";
            }
            
            matchScore = Math.max(1.0f, Math.min(100.0f, matchScore));
            
            recommendations.add(RecommendationDto.builder()
                    .challengeId(challenge.getId())
                    .title(challenge.getTitle())
                    .difficulty(challenge.getDifficulty())
                    .tags(challenge.getTags())
                    .aiDifficultyScore(aiDiff)
                    .matchScore(matchScore)
                    .reason(reason)
                    .build());
        }
        
        // Sort by match score descending
        recommendations.sort((a, b) -> Float.compare(b.getMatchScore(), a.getMatchScore()));
        
        return recommendations.stream().limit(count).collect(Collectors.toList());
    }
}
