package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BattleRoomRepository extends JpaRepository<BattleRoom, UUID> {
    // TODO: Add custom query methods.
}
