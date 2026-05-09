package com.codearena.module6_event.repository;

import com.codearena.module6_event.entity.EventInvitation;
import com.codearena.module6_event.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventInvitationRepository extends JpaRepository<EventInvitation, UUID> {

    Optional<EventInvitation> findFirstByEventIdAndParticipantId(UUID eventId, String participantId);

    List<EventInvitation> findByParticipantId(String participantId);

    List<EventInvitation> findByEventId(UUID eventId);

    List<EventInvitation> findByEventIdAndStatus(UUID eventId, InvitationStatus status);
}

