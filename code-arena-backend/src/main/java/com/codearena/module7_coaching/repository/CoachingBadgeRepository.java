package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.CoachingBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoachingBadgeRepository extends JpaRepository<CoachingBadge, UUID> {
    Optional<CoachingBadge> findByName(String name);
}
