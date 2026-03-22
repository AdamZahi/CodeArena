package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.CoachingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoachingSessionRepository extends JpaRepository<CoachingSession, UUID> {
    // TODO: Add custom query methods.
}
