package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleRoomRepository extends JpaRepository<BattleRoom, UUID>, JpaSpecificationExecutor<BattleRoom> {

    Optional<BattleRoom> findByInviteToken(String inviteToken);

    List<BattleRoom> findByStatusAndIsPublicTrue(BattleRoomStatus status);

    // Step 5: feed queries
    List<BattleRoom> findByStatusAndIsPublicTrueOrderByStartsAtDesc(BattleRoomStatus status, Pageable pageable);

    List<BattleRoom> findByStatusAndIsPublicTrueOrderByCreatedAtDesc(BattleRoomStatus status, Pageable pageable);

    @Query("SELECT b FROM BattleRoom b WHERE b.isPublic = true AND b.status = :status AND b.endsAt >= :since ORDER BY b.endsAt DESC")
    List<BattleRoom> findRecentFinishedPublic(BattleRoomStatus status, LocalDateTime since, Pageable pageable);

    long countByStatusAndIsPublicTrue(BattleRoomStatus status);

    // Step 5: stats queries
    long countByStatus(BattleRoomStatus status);

    @Query("SELECT b.mode, COUNT(b) FROM BattleRoom b GROUP BY b.mode")
    List<Object[]> countGroupedByMode();

    // ---------- Backoffice analytics & management queries ----------

    long countByCreatedAtBetween(Instant from, Instant to);

    long countByStatusAndCreatedAtBetween(BattleRoomStatus status, Instant from, Instant to);

    /**
     * Daily timeline of battles in [from, to] grouped by date.
     * Returns rows of (java.sql.Date day, long count).
     */
    @Query(value = "SELECT DATE(created_at) AS day, COUNT(*) AS cnt " +
            "FROM battle_room WHERE created_at BETWEEN :from AND :to " +
            "GROUP BY DATE(created_at) ORDER BY day ASC", nativeQuery = true)
    List<Object[]> timelineByDay(@Param("from") Instant from, @Param("to") Instant to);

    /** Average finished-battle duration in minutes (NULL-safe). */
    @Query(value = "SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, starts_at, ends_at)), 0) " +
            "FROM battle_room " +
            "WHERE status = 'FINISHED' " +
            "  AND starts_at IS NOT NULL AND ends_at IS NOT NULL", nativeQuery = true)
    Double averageDurationMinutes();

    @Query("SELECT COUNT(b) FROM BattleRoom b " +
            "WHERE b.status = com.codearena.module2_battle.enums.BattleRoomStatus.FINISHED " +
            "  AND b.startsAt IS NOT NULL AND b.endsAt IS NOT NULL")
    long countFinishedWithDurations();

    /** Rooms in IN_PROGRESS that started before {@code threshold}. */
    @Query("SELECT b FROM BattleRoom b " +
            "WHERE b.status = com.codearena.module2_battle.enums.BattleRoomStatus.IN_PROGRESS " +
            "  AND COALESCE(b.startsAt, FUNCTION('TIMESTAMP', b.createdAt)) < :threshold")
    List<BattleRoom> findStuckRooms(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT b FROM BattleRoom b WHERE b.createdAt BETWEEN :from AND :to ORDER BY b.createdAt ASC")
    List<BattleRoom> findInRange(@Param("from") Instant from, @Param("to") Instant to);
}
