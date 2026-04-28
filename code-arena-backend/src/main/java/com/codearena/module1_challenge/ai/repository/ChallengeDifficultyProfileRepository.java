package com.codearena.module1_challenge.ai.repository;

import com.codearena.module1_challenge.ai.entity.ChallengeDifficultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChallengeDifficultyProfileRepository extends JpaRepository<ChallengeDifficultyProfile, Long> {
    Optional<ChallengeDifficultyProfile> findByChallengeId(Long challengeId);
}
