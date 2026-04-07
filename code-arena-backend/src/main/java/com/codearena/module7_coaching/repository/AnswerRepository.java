package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {
    List<Answer> findByQuizAttemptId(UUID quizAttemptId);
}
