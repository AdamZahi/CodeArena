package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
    // TODO: Add custom query methods.
}
