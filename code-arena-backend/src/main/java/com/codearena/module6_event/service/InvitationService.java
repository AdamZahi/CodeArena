package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.InvitationResponseDTO;
import com.codearena.module6_event.dto.RegistrationResponseDTO;

import java.util.List;
import java.util.UUID;

public interface InvitationService {

    int inviteTop10Players(UUID eventId);

    List<InvitationResponseDTO> getMyInvitations(String participantId);

    RegistrationResponseDTO acceptInvitation(UUID eventId, String participantId);

    InvitationResponseDTO declineInvitation(UUID eventId, String participantId);
}

