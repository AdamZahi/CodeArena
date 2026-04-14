package com.codearena.module9_arenatalk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String senderName;

    private LocalDateTime sentAt;

    private boolean pinned;

    private LocalDateTime pinnedAt;

    @ManyToOne
    @JoinColumn(name = "channel_id")
    @JsonIgnore
    private TextChannel channel;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
        if (this.pinnedAt == null && this.pinned) {
            this.pinnedAt = LocalDateTime.now();
        }
    }
}