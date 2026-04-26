package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoomChallenge;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BattleRoomChallengeRepository extends JpaRepository<BattleRoomChallenge, UUID> {

    List<BattleRoomChallenge> findByRoomIdOrderByPositionAsc(String roomId);

    /** Top challenges by usage in battles. Returns rows of (challengeId, count). */
    @Query("SELECT brc.challengeId, COUNT(brc) FROM BattleRoomChallenge brc " +
            "GROUP BY brc.challengeId ORDER BY COUNT(brc) DESC")
    List<Object[]> findTopChallengesByUsage(Pageable pageable);

    @Modifying
    @Transactional
    void deleteByRoomId(String roomId);
}
