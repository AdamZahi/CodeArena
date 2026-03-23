package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.SubmissionDto;
import com.codearena.module1_challenge.dto.SubmitCodeRequest;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final ExecutionService executionService;

    @Override
    @Transactional
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

        // FATAL FIX: Force initialize the lazy testcases bag BEFORE moving to the separate @Async thread
        challenge.getTestCases().size();

        // TRUE Async execution of test cases via separate service
        executionService.executeSubmission(submission, challenge.getTestCases());

        return mapToDto(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDto getSubmissionStatus(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDto> getUserSubmissions(String userId) {
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDto> getChallengeSubmissions(Long challengeId) {
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
