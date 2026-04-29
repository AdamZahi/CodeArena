package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import com.codearena.module2_battle.dto.ComplexityClassificationResult;
import com.codearena.module2_battle.dto.PistonExecutionRequest;
import com.codearena.module2_battle.dto.PistonExecutionResult;
import com.codearena.module2_battle.exception.CodeExecutionUnavailableException;
import com.codearena.module2_battle.exception.UnsupportedLanguageException;
import com.codearena.module2_battle.service.ClassifierBridgeService;
import com.codearena.module2_battle.service.CodeWrapperService;
import com.codearena.module2_battle.service.PistonClient;
import com.codearena.module2_battle.util.PistonLanguageMapper;
import com.codearena.module2_battle.util.PistonLanguageMapper.PistonLang;
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

    private final PistonClient pistonClient;
    private final PistonLanguageMapper pistonLanguageMapper;
    private final CodeWrapperService codeWrapperService;
    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final XpCalculatorService xpCalculatorService;
    private final CustomizationService customizationService;
    private final ClassifierBridgeService classifierBridgeService;

    @Async
    @Transactional
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
                submission.setStatus("ACCEPTED");
                submissionRepository.save(submission);
                return;
            }

            PistonLang pistonLang;
            try {
                pistonLang = pistonLanguageMapper.toPistonLang(submission.getLanguage());
            } catch (UnsupportedLanguageException ex) {
                submission.setStatus("INTERNAL_ERROR");
                submission.setErrorOutput("Unsupported language: " + submission.getLanguage());
                submissionRepository.save(submission);
                return;
            }

            StringBuilder outputLog = new StringBuilder();
            String finalVerdict = "ACCEPTED";
            int passedTests = 0;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);

                // Wrap user code so the function defined in the submission is invoked with the
                // test-case input (LeetCode-style). Falls back to raw stdin if wrapping fails.
                String wrappedCode = codeWrapperService.wrapCode(
                        submission.getCode(), submission.getLanguage(), tc.getInput());
                boolean wrapped = wrappedCode != null;

                PistonExecutionRequest request = PistonExecutionRequest.builder()
                        .language(pistonLang.language())
                        .version(pistonLang.version())
                        .sourceCode(wrapped ? wrappedCode : submission.getCode())
                        .fileName(pistonFileName(pistonLang.language(), wrapped, submission.getCode()))
                        .stdin(wrapped ? null : tc.getInput())
                        .build();

                PistonExecutionResult result;
                try {
                    result = pistonClient.execute(request);
                } catch (CodeExecutionUnavailableException ex) {
                    submission.setStatus("INTERNAL_ERROR");
                    submission.setErrorOutput(outputLog + "\nCode execution service is temporarily unavailable — please retry.");
                    submissionRepository.save(submission);
                    return;
                }

                if (result.getCpuTimeMs() != null) {
                    totalExecTime += result.getCpuTimeMs() / 1000f;
                }
                Integer memKb = result.getMemoryKb();
                if (memKb != null) {
                    maxMemory = Math.max(maxMemory, memKb.floatValue());
                }

                String compileOutput = result.getCompileOutput() != null ? result.getCompileOutput().trim() : "";
                String stderr = result.getStderr() != null ? result.getStderr().trim() : "";
                String stdoutRaw = result.getStdout() != null ? result.getStdout() : "";
                String expectedRaw = tc.getExpectedOutput() != null ? tc.getExpectedOutput() : "";
                String stdout = normalizeOutput(stdoutRaw);
                String expectedOut = normalizeOutput(expectedRaw);

                outputLog.append("Test ").append(i + 1).append(": ");

                String verdict = mapPistonResult(result, expectedOut, stdout);

                switch (verdict) {
                    case "ACCEPTED" -> {
                        outputLog.append("Passed \u2713\n");
                        passedTests++;
                    }
                    case "WRONG_ANSWER" -> {
                        allPassed = false;
                        finalVerdict = "WRONG_ANSWER";
                        outputLog.append("WRONG_ANSWER | Expected: [").append(expectedOut)
                                .append("] Got: [").append(stdout).append("]\n");
                        submission.setErrorOutput(outputLog.toString());
                    }
                    default -> {
                        allPassed = false;
                        finalVerdict = verdict;
                        outputLog.append(verdict);
                        if (!compileOutput.isEmpty()) outputLog.append(" | ").append(compileOutput);
                        if (!stderr.isEmpty()) outputLog.append(" | ").append(stderr);
                        outputLog.append("\n");
                        submission.setErrorOutput(outputLog.toString());
                    }
                }

                if (!"ACCEPTED".equals(verdict)) {
                    break;
                }
            }

            if (allPassed) {
                submission.setStatus("ACCEPTED");
                submission.setErrorOutput(outputLog.toString());
                awardXpToUser(submission);
            } else {
                submission.setStatus(finalVerdict);
            }

            submission.setExecutionTime(totalExecTime);
            submission.setMemoryUsed(maxMemory);

            // Tag the submission with its predicted Big-O complexity. Done for
            // every verdict (not just ACCEPTED) so users get feedback on the
            // shape of their algorithm even when it fails functional tests.
            ComplexityClassificationResult complexity = classifierBridgeService
                    .classify(submission.getCode(), submission.getLanguage());
            submission.setComplexityLabel(complexity.getLabel());
            submission.setComplexityDisplay(complexity.getDisplay());
            submission.setComplexityScore(complexity.getScore());
            submission.setComplexityConfidence(complexity.getConfidence());

            submissionRepository.save(submission);

        } catch (Exception e) {
            log.error("Execution failed", e);
            sub.setStatus("INTERNAL_ERROR");
            sub.setErrorOutput(e.getMessage());
            submissionRepository.save(sub);
        }
    }

    /**
     * Returns the filename Piston should use for this submission. Only matters for Java, where
     * Piston's runner derives the main-class name from the file's basename. The wrapper always
     * generates a {@code class Main}, so wrapped Java goes to {@code Main.java}; unwrapped Java
     * uses the user's detected class name; everything else is left to Piston's defaults.
     */
    private String pistonFileName(String pistonLanguage, boolean wrapped, String userCode) {
        if (!"java".equals(pistonLanguage)) return null;
        if (wrapped) return "Main.java";
        String detected = CodeWrapperService.detectJavaClass(userCode);
        return (detected != null) ? detected + ".java" : "Main.java";
    }

    /**
     * Normalizes output for comparison: collapses CRLF/CR to LF, strips trailing whitespace from
     * each line, and drops trailing blank lines. Matches BattleArenaService's logic so a correct
     * solution isn't rejected for cosmetic newline differences between the test-case data and
     * Piston's stdout.
     */
    private String normalizeOutput(String value) {
        if (value == null) return "";
        String normalized = value.replace("\r\n", "\n").replace("\r", "\n");
        java.util.List<String> lines = new java.util.ArrayList<>(java.util.Arrays.asList(normalized.split("\n", -1)));
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, lines.get(i).stripTrailing());
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines);
    }

    /**
     * Maps a Piston execution result to a verdict string compatible with legacy Submission.status values.
     */
    private String mapPistonResult(PistonExecutionResult result, String expectedOutput, String stdout) {
        if (result.getErrorMessage() != null) {
            return "COMPILATION_ERROR";
        }
        if (result.getCompileOutput() != null && !result.getCompileOutput().isBlank()) {
            return "COMPILATION_ERROR";
        }
        String signal = result.getSignal();
        if (signal != null && !signal.isBlank()) {
            if ("SIGKILL".equals(signal) || "SIGXCPU".equals(signal)) {
                return "TIME_LIMIT_EXCEEDED";
            }
            return "RUNTIME_ERROR";
        }
        if (result.getExitCode() != 0) {
            return "RUNTIME_ERROR";
        }
        if (!stdout.equals(expectedOutput)) {
            return "WRONG_ANSWER";
        }
        return "ACCEPTED";
    }

    /**
     * Awards XP to the user after an ACCEPTED submission.
     */
    private void awardXpToUser(Submission submission) {
        try {
            String userId = submission.getUserId();
            if (userId == null || userId.isBlank()) {
                log.warn("No userId on submission {}, skipping XP award", submission.getId());
                return;
            }

            var challenge = challengeRepository.findById(submission.getChallenge().getId()).orElse(null);
            String difficulty = (challenge != null) ? challenge.getDifficulty() : null;
            int xpAmount = xpCalculatorService.calculateXp(difficulty, null);

            submission.setXpEarned(String.valueOf(xpAmount));

            User user = userRepository.findByAuth0Id(userId).orElse(null);
            if (user == null) {
                log.warn("User not found by auth0Id '{}', skipping XP award", userId);
                return;
            }

            long newTotalXp = (user.getTotalXp() != null ? user.getTotalXp() : 0L) + xpAmount;
            user.setTotalXp(newTotalXp);

            int newLevel = (int) (newTotalXp / 500) + 1;
            user.setLevel(newLevel);
            user.setCurrentLevel(newLevel);
            userRepository.save(user);

            log.info("Awarded {} XP to user {} (total: {}, level: {})", xpAmount, userId, newTotalXp, newLevel);

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
