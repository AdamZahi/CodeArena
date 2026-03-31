package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleRoomRepository extends JpaRepository<BattleRoom, UUID> {

    Optional<BattleRoom> findByInviteToken(String inviteToken);

    List<BattleRoom> findByStatusAndIsPublicTrue(BattleRoomStatus status);
}
