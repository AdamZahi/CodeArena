package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.CreateStoryChapterRequest;
import com.codearena.module8_terminalquest.dto.StoryChapterDto;
import com.codearena.module8_terminalquest.service.StoryChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/terminal-quest/chapters")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StoryChapterController {

    private final StoryChapterService storyChapterService;

    @GetMapping
    public ResponseEntity<List<StoryChapterDto>> getAllChapters() {
        return ResponseEntity.ok(storyChapterService.getAllChapters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryChapterDto> getChapterById(@PathVariable UUID id) {
        return ResponseEntity.ok(storyChapterService.getChapterById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StoryChapterDto> createChapter(@RequestBody CreateStoryChapterRequest request) {
        return ResponseEntity.ok(storyChapterService.createChapter(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StoryChapterDto> updateChapter(@PathVariable UUID id, @RequestBody CreateStoryChapterRequest request) {
        return ResponseEntity.ok(storyChapterService.updateChapter(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable UUID id) {
        storyChapterService.deleteChapter(id);
        return ResponseEntity.ok().build();
    }
}
