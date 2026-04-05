package com.codearena.module9_arenatalk.entity;

import com.codearena.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "arena_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArenNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String message;
    private String hubName;
    private Long hubId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean read = false;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}