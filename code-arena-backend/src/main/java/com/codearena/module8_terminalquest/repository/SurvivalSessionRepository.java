package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.SurvivalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SurvivalSessionRepository extends JpaRepository<SurvivalSession, UUID> {
    List<SurvivalSession> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT DISTINCT ss.userId FROM SurvivalSession ss")
    List<String> findDistinctUserIds();
}
