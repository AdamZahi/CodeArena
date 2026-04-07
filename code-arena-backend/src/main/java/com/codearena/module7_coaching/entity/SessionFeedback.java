package com.codearena.module7_coaching.entity;

import jakarta.persistence.*;
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
@Table(name = "session_feedbacks")
public class SessionFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String coachId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Added to satisfy existing DB NOT NULL constraint
    @Column(name = "session_id")
    @Builder.Default
    private UUID sessionId = UUID.randomUUID();

    @CreationTimestamp
    private Instant createdAt;
}
