package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.StoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryLevelRepository extends JpaRepository<StoryLevel, UUID> {
    List<StoryLevel> findByChapterIdOrderByOrderIndexAsc(UUID chapterId);
    List<StoryLevel> findByDifficulty(String difficulty);
}
