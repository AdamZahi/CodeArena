package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeVoteResponseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.ChallengeVote;
import com.codearena.module1_challenge.entity.VoteType;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.ChallengeVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeVoteServiceImpl implements ChallengeVoteService {

    private final ChallengeVoteRepository voteRepository;
    private final ChallengeRepository challengeRepository;

    @Override
    @Transactional
    public ChallengeVoteResponseDto upvote(Long challengeId, String userId) {
        return toggleVote(challengeId, userId, VoteType.UPVOTE);
    }

    @Override
    @Transactional
    public ChallengeVoteResponseDto downvote(Long challengeId, String userId) {
        return toggleVote(challengeId, userId, VoteType.DOWNVOTE);
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeVoteResponseDto getVotes(Long challengeId, String userId) {
        long upvotes = voteRepository.countByChallengeIdAndVoteType(challengeId, VoteType.UPVOTE);
        long downvotes = voteRepository.countByChallengeIdAndVoteType(challengeId, VoteType.DOWNVOTE);
        String userVote = voteRepository.findByChallengeIdAndUserId(challengeId, userId)
                .map(v -> v.getVoteType().name())
                .orElse(null);

        return ChallengeVoteResponseDto.builder()
                .upvotes(upvotes)
                .downvotes(downvotes)
                .userVote(userVote)
                .build();
    }

    private ChallengeVoteResponseDto toggleVote(Long challengeId, String userId, VoteType newVoteType) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found: " + challengeId));

        ChallengeVote existingVote = voteRepository.findByChallengeIdAndUserId(challengeId, userId).orElse(null);

        if (existingVote == null) {
            // New vote
            ChallengeVote vote = ChallengeVote.builder()
                    .challenge(challenge)
                    .userId(userId)
                    .voteType(newVoteType)
                    .build();
            voteRepository.save(vote);
        } else if (existingVote.getVoteType() == newVoteType) {
            // Remove vote if clicking the same one again
            voteRepository.delete(existingVote);
        } else {
            // Switch vote
            existingVote.setVoteType(newVoteType);
            voteRepository.save(existingVote);
        }

        return getVotes(challengeId, userId);
    }
}
