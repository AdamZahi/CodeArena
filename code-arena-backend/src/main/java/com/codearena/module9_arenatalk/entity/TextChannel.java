package com.codearena.module9_arenatalk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "text_channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String topic;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Message> messages;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}