package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.CoachApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoachApplicationRepository extends JpaRepository<CoachApplication, UUID> {
    List<CoachApplication> findByStatus(CoachApplication.ApplicationStatus status);
    List<CoachApplication> findAllByOrderByCreatedAtDesc();
    Optional<CoachApplication> findFirstByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndStatus(String userId, CoachApplication.ApplicationStatus status);
}
