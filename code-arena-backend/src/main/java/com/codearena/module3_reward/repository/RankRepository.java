package com.codearena.module3_reward.repository;

import com.codearena.module3_reward.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RankRepository extends JpaRepository<Rank, UUID> {
    // TODO: Add custom query methods.
}
