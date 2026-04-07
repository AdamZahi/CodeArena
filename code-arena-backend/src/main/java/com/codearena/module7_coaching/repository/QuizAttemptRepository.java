package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    List<QuizAttempt> findByUserId(String userId);
    List<QuizAttempt> findByUserIdOrderByCompletedAtDesc(String userId);
    List<QuizAttempt> findByQuizIdAndUserId(UUID quizId, String userId);
}
