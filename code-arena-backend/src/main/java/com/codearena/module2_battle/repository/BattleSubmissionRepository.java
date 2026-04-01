package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.BattleSubmission;
import com.codearena.module2_battle.enums.BattleSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BattleSubmissionRepository extends JpaRepository<BattleSubmission, UUID> {

    int countByParticipantIdAndRoomChallengeId(String participantId, String roomChallengeId);

    List<BattleSubmission> findByParticipantIdOrderBySubmittedAtAsc(String participantId);

    @Query("SELECT b FROM BattleSubmission b WHERE b.roomChallengeId IN " +
           "(SELECT CAST(brc.id AS string) FROM BattleRoomChallenge brc WHERE brc.challengeId = :challengeId) " +
           "AND b.status = 'ACCEPTED' AND b.runtimeMs IS NOT NULL AND b.memoryKb IS NOT NULL")
    List<BattleSubmission> findAllAcceptedWithMetricsByChallengeId(@Param("challengeId") String challengeId);

    // Step 5: stats and profile queries
    long countByStatus(BattleSubmissionStatus status);

    @Query("SELECT b.language, COUNT(b) FROM BattleSubmission b GROUP BY b.language")
    List<Object[]> countGroupedByLanguage();

    @Query("SELECT COUNT(b) FROM BattleSubmission b WHERE b.participantId IN :participantIds AND b.status = 'ACCEPTED'")
    long countAcceptedByParticipantIds(@Param("participantIds") List<String> participantIds);
}
