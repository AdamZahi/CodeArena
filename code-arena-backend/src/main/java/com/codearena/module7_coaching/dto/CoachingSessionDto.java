package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.SessionStatus;
import com.codearena.module7_coaching.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingSessionDto {
    private UUID id;
    private String coachId;
    private String learnerId;
    private String title;
    private String description;
    private ProgrammingLanguage language;
    private SkillLevel level;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private SessionStatus status;
    private String meetingUrl;
    private Integer maxParticipants;
    private Integer currentParticipants;
}
