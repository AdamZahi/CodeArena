package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdOrderBySubmittedAtDesc(String userId);
    List<Submission> findByChallengeIdOrderBySubmittedAtDesc(Long challengeId);
}
