package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<Season, UUID> {

    Optional<Season> findByIsActiveTrue();
}
