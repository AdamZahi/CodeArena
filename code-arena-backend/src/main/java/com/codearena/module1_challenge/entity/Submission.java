package com.codearena.module1_challenge.entity;

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
public class Submission {
    @Id
    private UUID id;

    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(columnDefinition = "TEXT")
    private String code;

    private String language; // e.g., JAVA, PYTHON, C, CPP, JAVASCRIPT

    private String status; // e.g., PENDING, ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR, TLE

    private String xpEarned;

    @CreationTimestamp
    private Instant submittedAt;

    private String judgeToken;

    private Float executionTime;

    private Float memoryUsed;

    @Column(columnDefinition = "TEXT")
    private String errorOutput;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
