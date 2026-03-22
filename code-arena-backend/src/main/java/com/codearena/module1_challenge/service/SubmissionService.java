package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.SubmissionDto;
import com.codearena.module1_challenge.dto.SubmitCodeRequest;

import java.util.List;
import java.util.UUID;

public interface SubmissionService {
    SubmissionDto submitCode(SubmitCodeRequest request, String userId);
    SubmissionDto getSubmissionStatus(UUID submissionId);
    List<SubmissionDto> getUserSubmissions(String userId);
    List<SubmissionDto> getChallengeSubmissions(UUID challengeId);
}
