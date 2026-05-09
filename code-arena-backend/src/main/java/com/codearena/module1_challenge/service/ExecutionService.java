package com.codearena.module1_challenge.service;

import com.codearena.execution.CodeExecutionService;
import com.codearena.execution.ExecutionRequest;
import com.codearena.execution.ExecutionResult;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final CodeExecutionService codeExecutionService;
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

                // Build execution request — CodeExecutionService handles engine selection
                ExecutionRequest execRequest = ExecutionRequest.builder()
                        .sourceCode(wrappedCode)
                        .language(submission.getLanguage())
                        .stdin(tc.getInput())
                        .expectedOutput(tc.getExpectedOutput())
                        .build();

                ExecutionResult result;
                try {
                    result = codeExecutionService.execute(execRequest);
                } catch (Exception e) {
                    log.error("Code execution failed for submission {}: {}", submission.getId(), e.getMessage());
                    submission.setStatus("INTERNAL_ERROR");
                    submission.setErrorOutput(outputLog.toString() + "\nCode execution service error: " + e.getMessage());
                    submissionRepository.save(submission);
                    return;
                }

                log.info("Test {}: exitCode={}, engine={}, time={}ms",
                        i + 1, result.getExitCode(), result.getEngineUsed(), result.getExecutionTimeMs());

                totalExecTime += result.getExecutionTimeMs() / 1000f;

                outputLog.append("Test ").append(i + 1).append(": ");

                // Check for compile errors
                if (result.getCompileError() != null && !result.getCompileError().isBlank()) {
                    allPassed = false;
                    finalVerdict = "COMPILATION_ERROR";
                    outputLog.append("COMPILATION_ERROR | ").append(result.getCompileError()).append("\n");
                    submission.setErrorOutput(outputLog.toString());
                    break;
                }

                // Check for runtime errors (non-zero exit code with no stdout match)
                if (result.getExitCode() != 0 && (result.getStderr() != null && !result.getStderr().isBlank())) {
                    allPassed = false;
                    finalVerdict = "RUNTIME_ERROR";
                    outputLog.append("RUNTIME_ERROR | ").append(result.getStderr()).append("\n");
                    submission.setErrorOutput(outputLog.toString());
                    break;
                }

                // Compare output — normalize and check
                String expectedOut = tc.getExpectedOutput() != null ? tc.getExpectedOutput().trim() : "";
                String actualOut = result.getStdout() != null ? result.getStdout().trim() : "";

                if (expectedOut.equals(actualOut)) {
                    outputLog.append("Passed ✓\n");
                    passedTests++;
                } else {
                    allPassed = false;
                    finalVerdict = "WRONG_ANSWER";
                    outputLog.append("WRONG_ANSWER | Expected: [").append(expectedOut)
                             .append("] Got: [").append(actualOut).append("]\n");
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
}
