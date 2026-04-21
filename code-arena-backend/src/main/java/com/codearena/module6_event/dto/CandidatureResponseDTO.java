package com.codearena.module6_event.dto;

import com.codearena.module6_event.enums.CandidatureStatus;
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
public class CandidatureResponseDTO {

    private UUID id;
    private UUID eventId;
    private String participantId;
    private String participantName;
    private String motivation;
    private CandidatureStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}

