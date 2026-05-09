package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.StoryChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryChapterRepository extends JpaRepository<StoryChapter, UUID> {
    List<StoryChapter> findAllByOrderByOrderIndexAsc();
}
