package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.CoachingSession;
import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.SessionStatus;
import com.codearena.module7_coaching.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoachingSessionRepository extends JpaRepository<CoachingSession, UUID> {
    List<CoachingSession> findByStatus(SessionStatus status);
    List<CoachingSession> findByLanguageAndLevel(ProgrammingLanguage language, SkillLevel level);
    List<CoachingSession> findByLanguageAndLevelAndStatus(ProgrammingLanguage language, SkillLevel level, SessionStatus status);
    List<CoachingSession> findByCoachId(String coachId);
    List<CoachingSession> findByLanguageInAndLevelInAndStatus(
        List<ProgrammingLanguage> languages, List<SkillLevel> levels, SessionStatus status);
}
