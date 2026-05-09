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
@EqualsAndHashCode(exclude = "chapter")
@ToString(exclude = "chapter")
@Entity
@Table(name = "story_mission")
public class StoryMission {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private StoryChapter chapter;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String context;

    private String task;

    @Column(columnDefinition = "TEXT")
    private String acceptedAnswers; // JSON array e.g. ["pwd", "pwd -L"]

    private String hint;

    private int orderIndex;

    private String difficulty; // EASY, MEDIUM, HARD

    private boolean isBoss;

    private int xpReward;

    @CreationTimestamp
    private Instant createdAt;
}
