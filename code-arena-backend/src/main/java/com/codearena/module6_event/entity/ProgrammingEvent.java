package com.codearena.module6_event.entity;

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
public class ProgrammingEvent {
    @Id
    private UUID id;

    private String title;

    private String description;

    private String organizerId;

    private String startDate;

    private String endDate;

    private String maxParticipants;

    private String status;
}
