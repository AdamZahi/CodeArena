package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.SurvivalLeaderboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SurvivalLeaderboardRepository extends JpaRepository<SurvivalLeaderboard, UUID> {
    Optional<SurvivalLeaderboard> findByUserId(String userId);
    List<SurvivalLeaderboard> findAllByOrderByBestWaveDescBestScoreDesc();
    Page<SurvivalLeaderboard> findAllByOrderByBestScoreDesc(Pageable pageable);
}
