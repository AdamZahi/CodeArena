package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.DailyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {

    Optional<DailyChallenge> findFirstByChallengeDate(LocalDate date);

    // Step 5: recency filter — get challenge IDs used in the last N days
    @Query("SELECT dc FROM DailyChallenge dc WHERE dc.challengeDate >= :since ORDER BY dc.challengeDate DESC")
    List<DailyChallenge> findRecentSince(@Param("since") LocalDate since);
}
