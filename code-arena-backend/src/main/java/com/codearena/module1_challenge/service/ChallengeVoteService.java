package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeVoteResponseDto;

public interface ChallengeVoteService {
    ChallengeVoteResponseDto upvote(Long challengeId, String userId);
    ChallengeVoteResponseDto downvote(Long challengeId, String userId);
    ChallengeVoteResponseDto getVotes(Long challengeId, String userId);
}
