package com.codearena.module7_coaching.entity;

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
public class Quiz {
    @Id
    private UUID id;

    private String title;

    private String description;

    private String difficulty;

    private String createdBy;

    @CreationTimestamp
    private Instant createdAt;
}
