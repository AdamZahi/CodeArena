package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.SubmissionDto;
import com.codearena.module1_challenge.dto.SubmitCodeRequest;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final Judge0Service judge0Service;

    @Override
    public SubmissionDto submitCode(SubmitCodeRequest request, String userId) {
        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new RuntimeException("Challenge not found: " + request.getChallengeId()));

        Submission submission = Submission.builder()
                .challenge(challenge)
                .userId(userId)
                .code(request.getCode())
                .language(request.getLanguage())
                .status("PENDING")
                .submittedAt(Instant.now())
                .build();

        submission = submissionRepository.save(submission);

        // Async execution of test cases
        executeSubmission(submission, challenge.getTestCases());

        return mapToDto(submission);
    }

    @Async
    protected void executeSubmission(Submission submission, List<TestCase> testCases) {
        try {
            boolean allPassed = true;
            Float totalExecTime = 0f;
            Float maxMemory = 0f;
            
            for (TestCase tc : testCases) {
                String token = judge0Service.submit(
                        submission.getCode(),
                        submission.getLanguage(),
                        tc.getExpectedOutput(),
                        tc.getInput()
                );

                if (token == null) {
                    submission.setStatus("INTERNAL_ERROR");
                    submissionRepository.save(submission);
                    return;
                }

                // Polling
                Map<String, Object> result = null;
                for (int i = 0; i < 15; i++) { // Poll 15 times, 2s each
                    Thread.sleep(2000);
                    result = judge0Service.getSubmissionStatus(token);
                    if (result != null && result.containsKey("status")) {
                        Map<String, Object> statusObj = (Map<String, Object>) result.get("status");
                        int statusId = (int) statusObj.get("id");
                        if (statusId != 1 && statusId != 2) { // Not In Queue or Processing
                            break;
                        }
                    }
                }

                if (result == null) {
                    submission.setStatus("TIMEOUT");
                    submissionRepository.save(submission);
                    return;
                }

                Map<String, Object> statusObj = (Map<String, Object>) result.get("status");
                int statusId = (int) statusObj.get("id");

                Object timeObj = result.get("time");
                if (timeObj != null) {
                    totalExecTime += Float.parseFloat(timeObj.toString());
                }
                
                Object memoryObj = result.get("memory");
                if (memoryObj != null) {
                    maxMemory = Math.max(maxMemory, Float.parseFloat(memoryObj.toString()));
                }

                if (statusId != 3) {
                    allPassed = false;
                    submission.setStatus(mapJudge0Status(statusId));
                    if (statusId == 6) { // Compilation Error
                        submission.setErrorOutput((String) result.get("compile_output"));
                    } else if (statusId > 6) { // Runtime error or similar
                        submission.setErrorOutput((String) result.get("stderr"));
                    }
                    break;
                }
            }

            if (allPassed) {
                submission.setStatus("ACCEPTED");
                submission.setXpEarned("10"); // arbitrary xp
            }
            
            submission.setExecutionTime(totalExecTime);
            submission.setMemoryUsed(maxMemory);
            submissionRepository.save(submission);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Execution interrupted", e);
            submission.setStatus("INTERNAL_ERROR");
            submissionRepository.save(submission);
        } catch (Exception e) {
            log.error("Execution failed", e);
            submission.setStatus("INTERNAL_ERROR");
            submissionRepository.save(submission);
        }
    }

    private String mapJudge0Status(int statusId) {
        switch (statusId) {
            case 3: return "ACCEPTED";
            case 4: return "WRONG_ANSWER";
            case 5: return "TLE";
            case 6: return "COMPILATION_ERROR";
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12: return "RUNTIME_ERROR";
            default: return "ERROR";
        }
    }

    @Override
    public SubmissionDto getSubmissionStatus(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
    }

    @Override
    public List<SubmissionDto> getUserSubmissions(String userId) {
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubmissionDto> getChallengeSubmissions(UUID challengeId) {
        return submissionRepository.findByChallengeIdOrderBySubmittedAtDesc(challengeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private SubmissionDto mapToDto(Submission sub) {
        return SubmissionDto.builder()
                .id(sub.getId())
                .challengeId(sub.getChallenge().getId())
                .userId(sub.getUserId())
                .code(sub.getCode())
                .language(sub.getLanguage())
                .status(sub.getStatus())
                .xpEarned(sub.getXpEarned())
                .submittedAt(sub.getSubmittedAt())
                .executionTime(sub.getExecutionTime())
                .memoryUsed(sub.getMemoryUsed())
                .errorOutput(sub.getErrorOutput())
                .challengeTitle(sub.getChallenge().getTitle())
                .build();
    }
}
