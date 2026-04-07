package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoomChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BattleRoomChallengeRepository extends JpaRepository<BattleRoomChallenge, UUID> {

    List<BattleRoomChallenge> findByRoomIdOrderByPositionAsc(String roomId);
}
