package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, UUID> {
    // TODO: Add custom query methods.
}
