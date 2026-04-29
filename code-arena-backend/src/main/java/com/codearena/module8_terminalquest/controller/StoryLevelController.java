package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.CreateStoryLevelRequest;
import com.codearena.module8_terminalquest.dto.StoryLevelDto;
import com.codearena.module8_terminalquest.service.StoryLevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/terminal-quest/levels")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StoryLevelController {

    private final StoryLevelService storyLevelService;

    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<List<StoryLevelDto>> getLevelsByChapter(@PathVariable UUID chapterId) {
        return ResponseEntity.ok(storyLevelService.getLevelsByChapter(chapterId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryLevelDto> getLevelById(@PathVariable UUID id) {
        return ResponseEntity.ok(storyLevelService.getLevelById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StoryLevelDto> createLevel(@Valid @RequestBody CreateStoryLevelRequest request) {
        return ResponseEntity.ok(storyLevelService.createLevel(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StoryLevelDto> updateLevel(@PathVariable UUID id, @Valid @RequestBody CreateStoryLevelRequest request) {
        return ResponseEntity.ok(storyLevelService.updateLevel(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLevel(@PathVariable UUID id) {
        storyLevelService.deleteLevel(id);
        return ResponseEntity.ok().build();
    }
}
