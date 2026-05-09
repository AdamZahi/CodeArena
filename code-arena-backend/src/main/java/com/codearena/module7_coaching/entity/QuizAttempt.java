package com.codearena.module7_coaching.entity;

import com.codearena.module7_coaching.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Enumerated(EnumType.STRING)
    private SkillLevel level;

    @Column(columnDefinition = "TEXT")
    private String weakTopics;

    @CreationTimestamp
    private Instant completedAt;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", insertable = false, updatable = false)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();
}
