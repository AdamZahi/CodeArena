package com.codearena.module9_arenatalk.entity;

import com.codearena.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hub_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hub_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    private LocalDateTime joinedAt;

    private boolean online;

    private LocalDateTime lastSeen;

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
        if (this.lastSeen == null) {
            this.lastSeen = LocalDateTime.now();
        }
    }
}