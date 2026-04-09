package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.service.CustomizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final Judge0Service judge0Service;
    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final XpCalculatorService xpCalculatorService;
    private final CustomizationService customizationService;

    @Async
    @Transactional
    @SuppressWarnings("unchecked")
    public void executeSubmission(Submission sub, List<TestCase> testCases) {
        try {
            // Prevent Race Condition: Wait for the main thread's INSERT transaction to commit
            Thread.sleep(800);
            
            Submission submission = submissionRepository.findById(sub.getId()).orElse(sub);

            boolean allPassed = true;
            Float totalExecTime = 0f;
            Float maxMemory = 0f;

            submission.setStatus("IN_PROGRESS");
            submissionRepository.save(submission);

            if (testCases == null || testCases.isEmpty()) {
                submission.setStatus("ACCEPTED"); // No test cases means pass
                submissionRepository.save(submission);
                return;
            }

            StringBuilder outputLog = new StringBuilder();
            String finalVerdict = "ACCEPTED";
            int passedTests = 0;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                
                // Wrap code like module3
                String wrappedCode = buildRunner(submission.getCode(), tc.getInput(), submission.getLanguage());

                String token = judge0Service.submit(
                        wrappedCode,
                        submission.getLanguage(),
                        tc.getExpectedOutput(),
                        tc.getInput()
                );

                if (token == null) {
                    submission.setStatus("INTERNAL_ERROR");
                    submission.setErrorOutput(outputLog.toString() + "\nJudge0 API refused the submission or returned null token. Wait 1 min or check your code.");
                    submissionRepository.save(submission);
                    return;
                }

                // Polling
                Map<String, Object> result = null;
                for (int attempt = 0; attempt < 30; attempt++) {
                    Thread.sleep(2000);
                    result = judge0Service.getSubmissionStatus(token);
                    if (result != null && result.get("status") instanceof Map) {
                        Map<String, Object> statusObj = (Map<String, Object>) result.get("status");
                        int statusId = ((Number) statusObj.get("id")).intValue();
                        if (statusId != 1 && statusId != 2) {
                            break;
                        }
                    }
                }

                if (result == null || !(result.get("status") instanceof Map)) {
                    submission.setStatus("TIMEOUT");
                    submissionRepository.save(submission);
                    return;
                }

                Map<String, Object> statusObj = (Map<String, Object>) result.get("status");
                int statusId = ((Number) statusObj.get("id")).intValue();
                String verdict = mapJudge0Status(statusId);

                Object timeObj = result.get("time");
                Object memoryObj = result.get("memory");
                log.info("Test {}: statusId={}, time={}, memory={}", i + 1, statusId, timeObj, memoryObj);

                if (timeObj != null) {
                    totalExecTime += Float.parseFloat(timeObj.toString());
                }

                if (memoryObj != null) {
                    maxMemory = Math.max(maxMemory, Float.parseFloat(memoryObj.toString()));
                }

                String compileOutput = judge0Service.decodeBase64((String) result.get("compile_output")).trim();
                String stderr = judge0Service.decodeBase64((String) result.get("stderr")).trim();
                String stdout = judge0Service.decodeBase64((String) result.get("stdout")).trim();

                outputLog.append("Test ").append(i + 1).append(": ");

                if (statusId == 3) {
                    // Judge0 confirmed: output matches expected — trust it
                    outputLog.append("Passed ✓\n");
                    passedTests++;
                } else if (statusId == 4) {
                    // Wrong Answer — show expected vs actual
                    allPassed = false;
                    finalVerdict = "WRONG_ANSWER";
                    String expectedOut = tc.getExpectedOutput() != null ? tc.getExpectedOutput().trim() : "";
                    outputLog.append("WRONG_ANSWER | Expected: [").append(expectedOut)
                             .append("] Got: [").append(stdout).append("]\n");
                    submission.setErrorOutput(outputLog.toString());
                    break;
                } else {
                    // Compilation error, runtime error, TLE, etc.
                    allPassed = false;
                    finalVerdict = verdict;
                    outputLog.append(verdict);
                    if (!compileOutput.isEmpty()) outputLog.append(" | ").append(compileOutput);
                    if (!stderr.isEmpty()) outputLog.append(" | ").append(stderr);
                    outputLog.append("\n");
                    submission.setErrorOutput(outputLog.toString());
                    break;
                }
            }

            if (allPassed) {
                submission.setStatus("ACCEPTED");
                submission.setErrorOutput(outputLog.toString());
                // === XP REWARD SYSTEM ===
                awardXpToUser(submission);
            } else {
                submission.setStatus(finalVerdict);
            }

            submission.setExecutionTime(totalExecTime);
            submission.setMemoryUsed(maxMemory);
            submissionRepository.save(submission);

        } catch (Exception e) {
            log.error("Execution failed", e);
            sub.setStatus("INTERNAL_ERROR");
            sub.setErrorOutput(e.getMessage());
            submissionRepository.save(sub);
        }
    }

    private String buildRunner(String userCode, String stdin, String languageId) {
        return userCode;
    }

    /**
     * Awards XP to the user after an ACCEPTED submission.
     * Calculates XP based on challenge difficulty, updates user totalXp + level,
     * and triggers cosmetic unlock checks.
     */
    private void awardXpToUser(Submission submission) {
        try {
            String userId = submission.getUserId();
            if (userId == null || userId.isBlank()) {
                log.warn("No userId on submission {}, skipping XP award", submission.getId());
                return;
            }

            // Get challenge difficulty for XP calculation
            var challenge = challengeRepository.findById(submission.getChallenge().getId()).orElse(null);
            String difficulty = (challenge != null) ? challenge.getDifficulty() : null;
            int xpAmount = xpCalculatorService.calculateXp(difficulty, null);

            submission.setXpEarned(String.valueOf(xpAmount));

            // Update user's totalXp and level
            User user = userRepository.findByAuth0Id(userId).orElse(null);
            if (user == null) {
                log.warn("User not found by auth0Id '{}', skipping XP award", userId);
                return;
            }

            long newTotalXp = (user.getTotalXp() != null ? user.getTotalXp() : 0L) + xpAmount;
            user.setTotalXp(newTotalXp);

            // Level up: every 500 XP = 1 level
            int newLevel = (int) (newTotalXp / 500) + 1;
            user.setLevel(newLevel);
            user.setCurrentLevel(newLevel);
            userRepository.save(user);

            log.info("Awarded {} XP to user {} (total: {}, level: {})", xpAmount, userId, newTotalXp, newLevel);

            // Trigger cosmetic unlock checks
            try {
                customizationService.checkAndGrantUnlocks(userId);
            } catch (Exception e) {
                log.warn("Cosmetic unlock check failed for user {}: {}", userId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to award XP for submission {}: {}", submission.getId(), e.getMessage());
        }
    }

    private String mapJudge0Status(int statusId) {
        switch (statusId) {
            case 3: return "ACCEPTED";
            case 4: return "WRONG_ANSWER";
            case 5: return "TIME_LIMIT_EXCEEDED";
            case 6: return "COMPILATION_ERROR";
            case 13: return "INTERNAL_ERROR";
            default: return "RUNTIME_ERROR";
        }
    }
}
