package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    // TODO: Add custom query methods.
}
