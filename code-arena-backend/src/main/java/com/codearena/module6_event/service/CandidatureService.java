package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.CandidatureResponseDTO;
import com.codearena.module6_event.dto.RegistrationResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CandidatureService {

    CandidatureResponseDTO submitCandidature(UUID eventId, String participantId, String motivation);

    List<CandidatureResponseDTO> getCandidaturesByEvent(UUID eventId);

    RegistrationResponseDTO acceptCandidature(UUID candidatureId);

    void rejectCandidature(UUID candidatureId);
}

