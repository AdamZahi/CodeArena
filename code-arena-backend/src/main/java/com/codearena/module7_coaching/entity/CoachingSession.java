package com.codearena.module7_coaching.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CoachingSession {
    @Id
    private UUID id;

    private String coachId;

    private String learnerId;

    private String scheduledAt;

    private String durationMinutes;

    private String status;

    private String meetingUrl;
}
