package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.ActivityLogDto;
import com.codearena.module8_terminalquest.entity.ActivityType;

import java.util.List;
import java.util.Map;

public interface ActivityLogService {
    void log(String userId, ActivityType type, String metadata);
    List<ActivityLogDto> getTimeline(String userId);
    List<ActivityLogDto> getTimelineByType(String userId, ActivityType type);
    Map<String, Long> getActivityBreakdown(String userId);
    Map<String, Long> getDailyActivity(String userId, int days);
}
