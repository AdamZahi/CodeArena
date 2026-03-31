package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.DailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, UUID> {

    Optional<DailyEntry> findByUserIdAndDailyChallengeId(String userId, String dailyChallengeId);
}
