package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.DailyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {

    Optional<DailyChallenge> findByChallengeDate(LocalDate date);
}
