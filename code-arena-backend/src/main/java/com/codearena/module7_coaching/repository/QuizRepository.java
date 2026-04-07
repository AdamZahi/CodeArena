package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.Quiz;
import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findByLanguage(ProgrammingLanguage language);
    List<Quiz> findByTitle(String title);
}
