package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.CreateStoryMissionRequest;
import com.codearena.module8_terminalquest.dto.StoryMissionDto;
import com.codearena.module8_terminalquest.service.StoryMissionService;
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
@RequestMapping("/api/terminal-quest/missions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StoryMissionController {

    private final StoryMissionService storyMissionService;

    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<List<StoryMissionDto>> getMissionsByChapter(@PathVariable UUID chapterId) {
        return ResponseEntity.ok(storyMissionService.getMissionsByChapter(chapterId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryMissionDto> getMissionById(@PathVariable UUID id) {
        return ResponseEntity.ok(storyMissionService.getMissionById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StoryMissionDto> createMission(@Valid @RequestBody CreateStoryMissionRequest request) {
        return ResponseEntity.ok(storyMissionService.createMission(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StoryMissionDto> updateMission(@PathVariable UUID id,
                                                          @Valid @RequestBody CreateStoryMissionRequest request) {
        return ResponseEntity.ok(storyMissionService.updateMission(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMission(@PathVariable UUID id) {
        storyMissionService.deleteMission(id);
        return ResponseEntity.ok().build();
    }
}
