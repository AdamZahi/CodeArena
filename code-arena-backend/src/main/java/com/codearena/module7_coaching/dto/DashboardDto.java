package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private Integer totalQuizzesTaken;
    private Double averageScore;
    private SkillLevel overallLevel;
    private List<UserSkillDto> skills;
    private List<CoachingSessionDto> recommendedSessions;
    private List<CoachingSessionDto> upcomingSessions;
    private List<BadgeDto> badges;
    private Long unreadNotifications;
}
