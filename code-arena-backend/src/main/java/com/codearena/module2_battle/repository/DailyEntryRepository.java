package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.DailyEntry;
import com.codearena.module2_battle.enums.DailyEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, UUID> {

    Optional<DailyEntry> findByUserIdAndDailyChallengeId(String userId, String dailyChallengeId);

    // Step 5: streak computation — all COMPLETED entries for a user, ordered by challenge date DESC
    @Query("SELECT de FROM DailyEntry de " +
           "JOIN DailyChallenge dc ON de.dailyChallengeId = CAST(dc.id AS string) " +
           "WHERE de.userId = :userId AND de.status = 'COMPLETED' " +
           "ORDER BY dc.challengeDate DESC")
    List<DailyEntry> findCompletedByUserOrderByDateDesc(@Param("userId") String userId);

    int countByUserId(String userId);

    // Step 5: daily leaderboard
    List<DailyEntry> findByDailyChallengeIdAndStatus(String dailyChallengeId, DailyEntryStatus status);

    // Step 5: stats
    long countByStatus(DailyEntryStatus status);
}
