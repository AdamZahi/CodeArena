package com.codearena.module1_challenge.ai.service;

import com.codearena.module1_challenge.ai.entity.ChallengeDifficultyProfile;
import com.codearena.module1_challenge.ai.repository.ChallengeDifficultyProfileRepository;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DifficultyPredictorService {

    private final ChallengeRepository challengeRepository;
    private final SubmissionRepository submissionRepository;
    private final ChallengeDifficultyProfileRepository profileRepository;

    @Transactional
    public void recalculateAll() {
        log.info("Starting AI difficulty recalculation for all challenges...");
        List<Challenge> challenges = challengeRepository.findAll();
        for (Challenge challenge : challenges) {
            try {
                analyzeChallengeData(challenge);
            } catch (Exception e) {
                log.error("Failed to analyze challenge {}: {}", challenge.getId(), e.getMessage());
            }
        }
        log.info("Finished AI difficulty recalculation.");
    }

    @Transactional
    public void analyzeChallengeData(Challenge challenge) {
        List<Submission> submissions = submissionRepository.findByChallengeIdOrderBySubmittedAtDesc(challenge.getId());
        
        ChallengeDifficultyProfile profile = profileRepository.findByChallengeId(challenge.getId())
                .orElse(ChallengeDifficultyProfile.builder()
                        .challenge(challenge)
                        .build());
                        
        if (submissions.isEmpty()) {
            profile.setAiDifficultyScore(getDefaultScore(challenge.getDifficulty()));
            profile.setSampleSize(0);
            profileRepository.save(profile);
            return;
        }

        int totalSubs = submissions.size();
        long acceptedCount = submissions.stream().filter(s -> "ACCEPTED".equals(s.getStatus())).count();
        long ceCount = submissions.stream().filter(s -> "COMPILATION_ERROR".equals(s.getStatus())).count();
        long waCount = submissions.stream().filter(s -> "WRONG_ANSWER".equals(s.getStatus())).count();
        long tleCount = submissions.stream().filter(s -> "TIME_LIMIT_EXCEEDED".equals(s.getStatus())).count();

        // Calculate unique users to find average attempts
        Map<String, List<Submission>> byUser = submissions.stream().collect(Collectors.groupingBy(Submission::getUserId));
        float avgAttempts = (float) totalSubs / byUser.size();

        // Calculate avg execution time for accepted
        double avgExecTime = submissions.stream()
                .filter(s -> "ACCEPTED".equals(s.getStatus()) && s.getExecutionTime() != null)
                .mapToDouble(Submission::getExecutionTime)
                .average()
                .orElse(0.0);

        float passRate = (float) acceptedCount / totalSubs;
        
        profile.setPassRate(passRate);
        profile.setAvgAttempts(avgAttempts);
        profile.setAvgExecutionTime((float) avgExecTime);
        profile.setCompilationErrorRate((float) ceCount / totalSubs);
        profile.setWrongAnswerRate((float) waCount / totalSubs);
        profile.setTleRate((float) tleCount / totalSubs);
        profile.setSampleSize(totalSubs);

        // Run Naive Bayes Prediction
        float predictedScore = predictDifficulty(profile, challenge.getDifficulty());
        profile.setAiDifficultyScore(predictedScore);
        
        profileRepository.save(profile);
    }

    private float predictDifficulty(ChallengeDifficultyProfile features, String humanDifficulty) {
        // Naive Bayes Simplified Heuristic
        // If sample size is too low, lean on human difficulty
        float baseScore = getDefaultScore(humanDifficulty);
        
        if (features.getSampleSize() < 3) {
            return baseScore;
        }

        // Adjust based on pass rate (lower pass rate = harder)
        // 100% pass rate = easier. 0% pass rate = harder (+30 diff)
        float passRateAdjustment = (0.5f - features.getPassRate()) * 40.0f; 

        // Adjust based on attempts (more attempts = harder)
        float attemptsAdjustment = Math.min((features.getAvgAttempts() - 1.0f) * 5.0f, 20.0f);

        // TLE rate indicates algorithmic complexity (harder)
        float tleAdjustment = features.getTleRate() * 30.0f;

        float score = baseScore + passRateAdjustment + attemptsAdjustment + tleAdjustment;
        
        return Math.max(1.0f, Math.min(100.0f, score)); // clamp 1-100
    }

    private float getDefaultScore(String difficulty) {
        if (difficulty == null) return 50.0f;
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> 20.0f;
            case "MEDIUM" -> 50.0f;
            case "HARD" -> 85.0f;
            default -> 50.0f;
        };
    }
}
