package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.ActivityLogDto;
import com.codearena.module8_terminalquest.entity.ActivityLog;
import com.codearena.module8_terminalquest.entity.ActivityType;
import com.codearena.module8_terminalquest.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Override
    @Transactional
    public void log(String userId, ActivityType type, String metadata) {
        activityLogRepository.save(ActivityLog.builder()
                .userId(userId)
                .activityType(type)
                .metadata(metadata)
                .build());
        log.debug("[activity] logged {} for user {}", type, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogDto> getTimeline(String userId) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogDto> getTimelineByType(String userId, ActivityType type) {
        return activityLogRepository.findByUserIdAndActivityTypeOrderByCreatedAtDesc(userId, type)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getActivityBreakdown(String userId) {
        Map<String, Long> result = new LinkedHashMap<>();
        activityLogRepository.countByActivityTypeForUser(userId)
                .forEach(row -> result.put(row[0].toString(), (Long) row[1]));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDailyActivity(String userId, int days) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            Instant since = Instant.now().minus((long) i + 1, ChronoUnit.DAYS);
            long count = activityLogRepository.countRecentActivity(userId, since);
            result.put("Day-" + i, count);
        }
        return result;
    }

    private ActivityLogDto toDto(ActivityLog a) {
        return ActivityLogDto.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .activityType(a.getActivityType())
                .metadata(a.getMetadata())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
