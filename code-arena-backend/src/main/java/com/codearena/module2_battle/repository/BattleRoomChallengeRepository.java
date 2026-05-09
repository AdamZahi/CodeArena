package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoomChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BattleRoomChallengeRepository extends JpaRepository<BattleRoomChallenge, UUID> {

    List<BattleRoomChallenge> findByRoomIdOrderByPositionAsc(String roomId);
    @Query("SELECT brc.challengeId, COUNT(brc) FROM BattleRoomChallenge brc GROUP BY brc.challengeId ORDER BY COUNT(brc) DESC")
    List<Object[]> findTopChallengesByUsage(Pageable pageable);

    void deleteByRoomId(String roomId);
}
