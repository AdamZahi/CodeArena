package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.LevelProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LevelProgressRepository extends JpaRepository<LevelProgress, UUID> {
    Optional<LevelProgress> findByUserIdAndLevelId(String userId, UUID levelId);
    List<LevelProgress> findByUserId(String userId);
    List<LevelProgress> findByUserIdAndCompleted(String userId, boolean completed);

    @Query("SELECT COALESCE(SUM(lp.starsEarned), 0) FROM LevelProgress lp WHERE lp.userId = :userId AND lp.completed = true")
    int sumStarsEarnedByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT lp.userId FROM LevelProgress lp")
    List<String> findDistinctUserIds();

    @Query("SELECT COUNT(lp) FROM LevelProgress lp WHERE lp.level.chapter.id = :chapterId")
    long countByChapterId(@Param("chapterId") UUID chapterId);

    @Query("SELECT COUNT(lp) FROM LevelProgress lp WHERE lp.level.chapter.id = :chapterId AND lp.completed = true")
    long countCompletedByChapterId(@Param("chapterId") UUID chapterId);
}
