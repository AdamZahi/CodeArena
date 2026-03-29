package com.codearena.module6_event.dto;

import com.codearena.module6_event.enums.EventCategory;
import com.codearena.module6_event.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

    private UUID id;
    private String title;
    private String description;
    private String location;
    private String organizerId;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private EventType type;
    private EventCategory category;
    private LocalDateTime createdAt;
    private Integer availablePlaces;
    private Boolean isFull;
    private Double fillRate;
}
