package com.codearena.module6_event.dto;

import com.codearena.module6_event.enums.EventStatus;
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
public class RegistrationResponseDTO {

    private UUID id;
    private String participantId;
    private UUID eventId;
    private EventStatus status;
    private String qrCode;
    private LocalDateTime registeredAt;
}
