package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.PlayerRating;
import com.codearena.module2_battle.enums.PlayerTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRatingRepository extends JpaRepository<PlayerRating, UUID> {

    Optional<PlayerRating> findByUserIdAndSeasonId(String userId, String seasonId);

    List<PlayerRating> findBySeasonIdOrderByEloDesc(String seasonId);

    // Step 5: leaderboard pagination
    Page<PlayerRating> findBySeasonIdOrderByEloDescWinsDesc(String seasonId, Pageable pageable);

    Page<PlayerRating> findBySeasonIdAndTierOrderByEloDescWinsDesc(String seasonId, PlayerTier tier, Pageable pageable);

    @Query("SELECT COUNT(pr) FROM PlayerRating pr WHERE pr.seasonId = :seasonId AND pr.elo > :elo")
    long countPlayersAboveElo(@Param("seasonId") String seasonId, @Param("elo") int elo);

    // Step 5: profile — best win streak across all seasons
    @Query("SELECT COALESCE(MAX(pr.bestWinStreak), 0) FROM PlayerRating pr WHERE pr.userId = :userId")
    int findBestWinStreakByUserId(@Param("userId") String userId);

    // Step 5: season management — all ratings for a season
    List<PlayerRating> findBySeasonId(String seasonId);

    // Step 5: stats — active streak count (users with >= 3 completed daily entries)
    @Query("SELECT COUNT(DISTINCT de.userId) FROM DailyEntry de WHERE de.status = 'COMPLETED' AND de.userId IN (SELECT de2.userId FROM DailyEntry de2 WHERE de2.status = 'COMPLETED' GROUP BY de2.userId HAVING COUNT(de2.id) >= 3)")
    long countUsersWithStreakAtLeast3();
}
