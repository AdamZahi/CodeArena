package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.StoryMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryMissionRepository extends JpaRepository<StoryMission, UUID> {
    List<StoryMission> findByChapterIdOrderByOrderIndexAsc(UUID chapterId);
    List<StoryMission> findByDifficulty(String difficulty);
}
