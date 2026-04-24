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
    @JoinColumn(name = "challenge_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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

    /** Big-O class predicted by the complexity classifier (e.g. "O1", "Onlogn"). */
    @Column(name = "complexity_label", length = 20)
    private String complexityLabel;

    /** Pretty form of the predicted Big-O class (e.g. "O(n log n)"). */
    @Column(name = "complexity_display", length = 20)
    private String complexityDisplay;

    /** Score in [0, 100] derived from the predicted Big-O class. */
    @Column(name = "complexity_score")
    private Double complexityScore;

    /** Softmax confidence of the predicted class, in [0, 1]. */
    @Column(name = "complexity_confidence")
    private Double complexityConfidence;
}
