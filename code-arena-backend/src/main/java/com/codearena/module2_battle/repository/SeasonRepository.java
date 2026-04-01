package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<Season, UUID> {

    Optional<Season> findByIsActiveTrue();

    // Step 5: season history — all seasons ordered by creation date DESC
    List<Season> findAllByOrderByCreatedAtDesc();
}
