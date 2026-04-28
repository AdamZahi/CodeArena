package com.codearena.module8_terminalquest.controller;

import com.codearena.module8_terminalquest.dto.ActivityLogDto;
import com.codearena.module8_terminalquest.entity.ActivityType;
import com.codearena.module8_terminalquest.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminal-quest/activity")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("/timeline/me")
    public ResponseEntity<List<ActivityLogDto>> getTimeline(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(activityLogService.getTimeline(jwt.getSubject()));
    }

    @GetMapping("/timeline/me/type/{type}")
    public ResponseEntity<List<ActivityLogDto>> getTimelineByType(
            @PathVariable ActivityType type,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(activityLogService.getTimelineByType(jwt.getSubject(), type));
    }

    @GetMapping("/breakdown/me")
    public ResponseEntity<Map<String, Long>> getActivityBreakdown(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(activityLogService.getActivityBreakdown(jwt.getSubject()));
    }

    @GetMapping("/daily/me")
    public ResponseEntity<Map<String, Long>> getDailyActivity(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(activityLogService.getDailyActivity(jwt.getSubject(), days));
    }
}
