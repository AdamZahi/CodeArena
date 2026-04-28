package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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


    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, b.startsAt, b.endsAt)) FROM BattleRoom b WHERE b.endsAt IS NOT NULL AND b.startsAt IS NOT NULL")
    Double averageDurationMinutes();

    @Query(value = "SELECT DATE(created_at), COUNT(*) FROM battle_room WHERE created_at BETWEEN :from AND :to GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> timelineByDay(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);
    @Query("SELECT COUNT(b) FROM BattleRoom b WHERE b.endsAt IS NOT NULL AND b.startsAt IS NOT NULL AND b.status = com.codearena.module2_battle.enums.BattleRoomStatus.FINISHED")
    long countFinishedWithDurations();



    @Query("SELECT b FROM BattleRoom b WHERE b.status = 'IN_PROGRESS' AND b.startsAt < :threshold")
    List<BattleRoom> findStuckRooms(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT b FROM BattleRoom b WHERE b.createdAt BETWEEN :from AND :to")
    List<BattleRoom> findInRange(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    @Query("SELECT COUNT(b) FROM BattleRoom b WHERE b.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);
}
