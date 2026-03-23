package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final Judge0Service judge0Service;
    private final SubmissionRepository submissionRepository;

    @Async
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
                if (timeObj != null) {
                    totalExecTime += Float.parseFloat(timeObj.toString());
                }

                Object memoryObj = result.get("memory");
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
                submission.setXpEarned("10");
                submission.setErrorOutput(outputLog.toString());
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
        // User writes a complete program that reads from stdin and prints to stdout.
        // Judge0 handles stdin injection and output comparison.
        return userCode;
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
