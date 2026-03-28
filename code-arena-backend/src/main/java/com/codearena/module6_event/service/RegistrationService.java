package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.RegistrationResponseDTO;

import java.util.List;
import java.util.UUID;

public interface RegistrationService {

    RegistrationResponseDTO register(UUID eventId, String participantId);

    void cancelRegistration(UUID eventId, String participantId);

    List<RegistrationResponseDTO> getEventParticipants(UUID eventId);

    List<RegistrationResponseDTO> getMyRegistrations(String participantId);
}
