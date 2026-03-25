package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.ChallengeVote;
import com.codearena.module1_challenge.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChallengeVoteRepository extends JpaRepository<ChallengeVote, Long> {
    Optional<ChallengeVote> findByChallengeIdAndUserId(Long challengeId, String userId);
    
    long countByChallengeIdAndVoteType(Long challengeId, VoteType voteType);
}
