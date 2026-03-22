package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    // TODO: Add custom query methods.
}
