package com.codearena.module3_reward.repository;

import com.codearena.module3_reward.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    // TODO: Add custom query methods.
}
