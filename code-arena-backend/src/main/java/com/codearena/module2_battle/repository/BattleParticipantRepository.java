package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.enums.ParticipantRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, UUID> {

    List<BattleParticipant> findByRoomIdAndRole(String roomId, ParticipantRole role);

    Optional<BattleParticipant> findByRoomIdAndUserId(String roomId, String userId);

    int countByRoomIdAndRole(String roomId, ParticipantRole role);

    // Step 5: profile and history queries
    List<BattleParticipant> findByUserId(String userId);

    @Query("SELECT bp FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
           "WHERE bp.userId = :userId AND br.status = 'FINISHED' AND bp.role = 'PLAYER' " +
           "ORDER BY bp.joinedAt DESC")
    List<BattleParticipant> findFinishedByUserIdOrderByJoinedAtDesc(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT COUNT(bp) FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
           "WHERE bp.userId = :userId AND br.status = 'FINISHED' AND bp.role = 'PLAYER'")
    long countFinishedByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(bp) FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
           "WHERE bp.userId = :userId AND br.status = 'FINISHED' AND bp.role = 'PLAYER' " +
           "AND br.mode IN ('DUEL', 'TEAM', 'RANKED_ARENA')")
    long countFinishedRankedByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(bp) FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
           "WHERE bp.userId = :userId AND br.status = 'FINISHED' AND bp.role = 'PLAYER' AND bp.rank = 1")
    long countWinsByUserId(@Param("userId") String userId);

    @Query("SELECT COALESCE(AVG(bp.score), 0) FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
           "WHERE bp.userId = :userId AND br.status = 'FINISHED' AND bp.role = 'PLAYER'")
    double averageScoreByUserId(@Param("userId") String userId);

    @Query("SELECT COALESCE(SUM(bp.score), 0) FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
           "WHERE bp.userId = :userId AND br.status = 'FINISHED' AND bp.role = 'PLAYER'")
    long sumScoreByUserId(@Param("userId") String userId);

    // ---------- Backoffice analytics queries ----------

    /** Top players: returns rows of (userId, battlesPlayed, battlesWon). */
    @Query("SELECT bp.userId, COUNT(bp), SUM(CASE WHEN bp.rank = 1 THEN 1 ELSE 0 END) " +
            "FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
            "WHERE br.status = com.codearena.module2_battle.enums.BattleRoomStatus.FINISHED " +
            "  AND bp.role = com.codearena.module2_battle.enums.ParticipantRole.PLAYER " +
            "  AND bp.userId IS NOT NULL " +
            "GROUP BY bp.userId " +
            "ORDER BY SUM(CASE WHEN bp.rank = 1 THEN 1 ELSE 0 END) DESC, COUNT(bp) DESC")
    List<Object[]> findTopPlayersByWins(Pageable pageable);

    long countByRoomId(String roomId);

    List<BattleParticipant> findByRoomId(String roomId);

    @Query("SELECT COUNT(DISTINCT bp.userId) FROM BattleParticipant bp WHERE bp.userId IS NOT NULL")
    long countDistinctParticipants();

    /** Total wins across the platform: any PLAYER with rank=1 in a FINISHED room. */
    @Query("SELECT COUNT(bp) FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
            "WHERE br.status = com.codearena.module2_battle.enums.BattleRoomStatus.FINISHED " +
            "  AND bp.role = com.codearena.module2_battle.enums.ParticipantRole.PLAYER " +
            "  AND bp.rank = 1")
    long countGlobalWins();

    /** Total finished player slots across the platform. */
    @Query("SELECT COUNT(bp) FROM BattleParticipant bp JOIN BattleRoom br ON bp.roomId = CAST(br.id AS string) " +
            "WHERE br.status = com.codearena.module2_battle.enums.BattleRoomStatus.FINISHED " +
            "  AND bp.role = com.codearena.module2_battle.enums.ParticipantRole.PLAYER")
    long countGlobalFinishedSlots();
}
