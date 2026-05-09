package com.codearena.module8_terminalquest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"levels", "missions"})
@ToString(exclude = {"levels", "missions"})
@Entity
@Table(name = "story_chapter")
public class StoryChapter {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int orderIndex;

    private boolean isLocked;

    private String speakerName;

    private String speakerVoice;

    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StoryLevel> levels = new ArrayList<>();

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StoryMission> missions = new ArrayList<>();
}
