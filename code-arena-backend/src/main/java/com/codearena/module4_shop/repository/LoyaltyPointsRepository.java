package com.codearena.module4_shop.repository;

import com.codearena.module4_shop.entity.LoyaltyPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, String> {
    Optional<LoyaltyPoints> findByParticipantId(String participantId);
}
