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
@Table(name = "survival_session")
public class SurvivalSession {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String userId;

    private int waveReached;

    private int score;

    private int livesRemaining;

    private String startedAt;

    private String endedAt;

    @CreationTimestamp
    private Instant createdAt;
}
