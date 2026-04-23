package com.codearena.module6_event.dto;

import com.codearena.module6_event.enums.EventCategory;
import com.codearena.module6_event.enums.EventType;
import jakarta.validation.constraints.*;
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

    @NotBlank
    @Size(min = 3, max = 100)
    private String title;

    @NotBlank
    private String description;

    private String location;
    
    private String organizerId;

    @NotNull
    @Future
    private LocalDateTime startDate;

    @NotNull
    @Future
    private LocalDateTime endDate;

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer maxParticipants;

    @NotNull
    private EventType type;

    @NotNull
    private EventCategory category;

    @Builder.Default
    private String status = "UPCOMING";
}

