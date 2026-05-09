package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoachRepository extends JpaRepository<Coach, UUID> {
    Optional<Coach> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
