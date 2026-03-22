package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeDto;
import com.codearena.module1_challenge.dto.CreateChallengeRequest;

import java.util.List;
import java.util.UUID;

public interface ChallengeService {
    List<ChallengeDto> getAllChallenges();
    ChallengeDto getChallengeById(UUID id);
    ChallengeDto createChallenge(CreateChallengeRequest request, String authorId);
    void deleteChallenge(UUID id);
}
