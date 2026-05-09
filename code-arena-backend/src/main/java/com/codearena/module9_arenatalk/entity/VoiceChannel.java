package com.codearena.module9_arenatalk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voice_channels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoiceChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int maxParticipants;

    @ManyToOne
    @JoinColumn(name = "hub_id", nullable = false)
    @JsonIgnore
    private Hub hub;
}