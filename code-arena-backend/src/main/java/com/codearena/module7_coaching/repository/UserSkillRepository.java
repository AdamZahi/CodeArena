package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.UserSkill;
import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {
    List<UserSkill> findByUserId(String userId);
    Optional<UserSkill> findByUserIdAndLanguage(String userId, ProgrammingLanguage language);
    List<UserSkill> findByUserIdAndLevel(String userId, SkillLevel level);
}
