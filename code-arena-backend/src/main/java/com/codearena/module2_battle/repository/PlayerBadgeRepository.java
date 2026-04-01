package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.PlayerBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerBadgeRepository extends JpaRepository<PlayerBadge, UUID> {

    List<PlayerBadge> findByUserId(String userId);

    boolean existsByUserIdAndBadgeId(String userId, String badgeId);

    // Step 5: badges earned in a specific match
    List<PlayerBadge> findByParticipantId(String participantId);

    // Step 5: profile — ordered by most recent first
    List<PlayerBadge> findByUserIdOrderByAwardedAtDesc(String userId);
}
