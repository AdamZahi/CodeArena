package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleRoomRepository extends JpaRepository<BattleRoom, UUID> {

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
}
