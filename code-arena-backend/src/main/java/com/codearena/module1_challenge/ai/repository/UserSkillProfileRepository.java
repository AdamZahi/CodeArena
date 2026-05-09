package com.codearena.module1_challenge.ai.repository;

import com.codearena.module1_challenge.ai.entity.UserSkillProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSkillProfileRepository extends JpaRepository<UserSkillProfile, Long> {
    Optional<UserSkillProfile> findByUserId(String userId);
}
