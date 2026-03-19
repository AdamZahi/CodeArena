package com.codearena.module1_challenge.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Challenge {
    @Id
    private UUID id;

    private String title;

    private String description;

    private String difficulty;

    private String tags;

    private String authorId;

    @CreationTimestamp
    private Instant createdAt;
}
