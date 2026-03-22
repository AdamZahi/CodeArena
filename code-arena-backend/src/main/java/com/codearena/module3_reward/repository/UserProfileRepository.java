package com.codearena.module3_reward.repository;

import com.codearena.module3_reward.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    // TODO: Add custom query methods.
}
