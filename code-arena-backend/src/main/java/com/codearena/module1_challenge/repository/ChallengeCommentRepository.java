package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.ChallengeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeCommentRepository extends JpaRepository<ChallengeComment, Long> {
    List<ChallengeComment> findByChallengeIdOrderByCreatedAtDesc(Long challengeId);
}
