package com.codearena.module1_challenge.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_skill_profile")
public class UserSkillProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String skillVector;

    @Builder.Default
    private Float overallSkillRating = 0.0f;

    @Builder.Default
    private Integer totalSolved = 0;

    @Builder.Default
    private Integer totalAttempted = 0;

    @UpdateTimestamp
    private Instant lastCalculatedAt;
}
