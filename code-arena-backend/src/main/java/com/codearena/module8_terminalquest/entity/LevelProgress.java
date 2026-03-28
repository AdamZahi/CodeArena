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
@EqualsAndHashCode(exclude = "level")
@ToString(exclude = "level")
@Entity
@Table(name = "level_progress")
public class LevelProgress {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    private StoryLevel level;

    private boolean completed;

    private int starsEarned;

    private int attempts;

    private String completedAt;

    @CreationTimestamp
    private Instant createdAt;
}
