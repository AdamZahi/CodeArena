package com.codearena.module8_terminalquest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String userId;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    private Instant createdAt;
}
