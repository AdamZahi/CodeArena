package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.ActivityLogDto;
import com.codearena.module8_terminalquest.entity.ActivityType;
import com.codearena.module8_terminalquest.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminal-quest/activity")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("/timeline/{userId}")
    public ResponseEntity<List<ActivityLogDto>> getTimeline(@PathVariable String userId) {
        return ResponseEntity.ok(activityLogService.getTimeline(userId));
    }

    @GetMapping("/timeline/{userId}/type/{type}")
    public ResponseEntity<List<ActivityLogDto>> getTimelineByType(
            @PathVariable String userId,
            @PathVariable ActivityType type) {
        return ResponseEntity.ok(activityLogService.getTimelineByType(userId, type));
    }

    @GetMapping("/breakdown/{userId}")
    public ResponseEntity<Map<String, Long>> getActivityBreakdown(@PathVariable String userId) {
        return ResponseEntity.ok(activityLogService.getActivityBreakdown(userId));
    }

    @GetMapping("/daily/{userId}")
    public ResponseEntity<Map<String, Long>> getDailyActivity(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(activityLogService.getDailyActivity(userId, days));
    }
}
