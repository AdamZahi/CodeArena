package com.codearena.module6_event.dto;

import com.codearena.module6_event.enums.EventCategory;
import com.codearena.module6_event.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    private String title;
    private String description;
    private String location;
    private String organizerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private EventType type;
    private EventCategory category;

    @Builder.Default
    private String status = "UPCOMING";
}
