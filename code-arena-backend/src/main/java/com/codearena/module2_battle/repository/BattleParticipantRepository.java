package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.enums.ParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, UUID> {

    List<BattleParticipant> findByRoomIdAndRole(String roomId, ParticipantRole role);

    Optional<BattleParticipant> findByRoomIdAndUserId(String roomId, String userId);

    int countByRoomIdAndRole(String roomId, ParticipantRole role);
}
