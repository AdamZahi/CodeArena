package com.codearena.module3_reward.repository;

import com.codearena.module3_reward.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    // TODO: Add custom query methods.
}
