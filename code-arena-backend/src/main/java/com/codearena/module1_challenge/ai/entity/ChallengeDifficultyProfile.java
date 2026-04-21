package com.codearena.module1_challenge.ai.entity;

import com.codearena.module1_challenge.entity.Challenge;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "challenge_difficulty_profile")
public class ChallengeDifficultyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false, unique = true, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Challenge challenge;

    @Builder.Default
    @Column(name = "ai_difficulty_score", nullable = false)
    private Float aiDifficultyScore = 50.0f; // 1-100

    @Builder.Default
    private Float passRate = 0.0f;
    
    @Builder.Default
    private Float avgAttempts = 0.0f;
    
    @Builder.Default
    private Float avgExecutionTime = 0.0f;
    
    @Builder.Default
    private Float compilationErrorRate = 0.0f;
    
    @Builder.Default
    private Float wrongAnswerRate = 0.0f;
    
    @Builder.Default
    private Float tleRate = 0.0f;

    @Builder.Default
    private Integer sampleSize = 0;

    @UpdateTimestamp
    private Instant lastCalculatedAt;
}
