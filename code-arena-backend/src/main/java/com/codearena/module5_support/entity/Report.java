package com.codearena.module5_support.entity;

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
public class Report {
    @Id
    private UUID id;

    private String reporterId;

    private String targetType;

    private String targetId;

    private String reason;

    private String status;

    @CreationTimestamp
    private Instant createdAt;
}
