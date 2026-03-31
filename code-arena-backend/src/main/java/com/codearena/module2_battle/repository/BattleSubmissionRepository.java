package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BattleSubmissionRepository extends JpaRepository<BattleSubmission, UUID> {

    int countByParticipantIdAndRoomChallengeId(String participantId, String roomChallengeId);

    List<BattleSubmission> findByParticipantIdOrderBySubmittedAtAsc(String participantId);
}
