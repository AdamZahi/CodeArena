package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.StoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryLevelRepository extends JpaRepository<StoryLevel, UUID> {
    List<StoryLevel> findByChapterIdOrderByOrderIndexAsc(UUID chapterId);
    List<StoryLevel> findByDifficulty(String difficulty);

    @Query("SELECT COUNT(lp) FROM LevelProgress lp WHERE lp.level IS NOT NULL AND lp.level.difficulty = :difficulty")
    long countAttemptsByDifficulty(@Param("difficulty") String difficulty);

    @Query("SELECT COUNT(lp) FROM LevelProgress lp WHERE lp.level IS NOT NULL AND lp.level.difficulty = :difficulty AND lp.completed = true")
    long countCompletionsByDifficulty(@Param("difficulty") String difficulty);
}
