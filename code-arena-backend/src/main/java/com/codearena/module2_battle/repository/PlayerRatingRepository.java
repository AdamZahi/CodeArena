package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.PlayerRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRatingRepository extends JpaRepository<PlayerRating, UUID> {

    Optional<PlayerRating> findByUserIdAndSeasonId(String userId, String seasonId);

    List<PlayerRating> findBySeasonIdOrderByEloDesc(String seasonId);
}
