package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.SessionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, UUID> {
    List<SessionFeedback> findByCoachId(String coachId);
    List<SessionFeedback> findByUserId(String userId);
}
