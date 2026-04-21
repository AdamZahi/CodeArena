package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByQuizId(UUID quizId);

    @Modifying
    @Transactional
    void deleteByQuizId(UUID quizId);
}
