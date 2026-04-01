package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    // Step 5: daily challenge generation
    List<Challenge> findByDifficulty(String difficulty);
}
