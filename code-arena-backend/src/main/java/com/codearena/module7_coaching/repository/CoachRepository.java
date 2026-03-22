package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoachRepository extends JpaRepository<Coach, UUID> {
    // TODO: Add custom query methods.
}
