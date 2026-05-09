package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {
    List<Answer> findByQuizAttemptId(UUID quizAttemptId);

    @Modifying
    @Transactional
    void deleteByQuizAttemptId(UUID quizAttemptId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM answers WHERE quiz_attempt_id IN (SELECT id FROM quiz_attempts WHERE quiz_id = :quizId)", nativeQuery = true)
    void deleteByQuizId(@Param("quizId") UUID quizId);
}
