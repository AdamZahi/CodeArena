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
@EqualsAndHashCode(exclude = {"level", "mission"})
@ToString(exclude = {"level", "mission"})
@Entity
@Table(name = "level_progress")
public class LevelProgress {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = true)
    private StoryLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = true)
    private StoryMission mission;

    private boolean completed;

    private int starsEarned;

    private int attempts;

    private String completedAt;

    @CreationTimestamp
    private Instant createdAt;
}
