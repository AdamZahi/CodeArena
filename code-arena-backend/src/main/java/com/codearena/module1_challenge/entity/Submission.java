package com.codearena.module1_challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "challenge")
@ToString(exclude = "challenge")
@Entity
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
