package com.codearena.module6_event.repository;

import com.codearena.module6_event.entity.EventCandidature;
import com.codearena.module6_event.enums.CandidatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventCandidatureRepository extends JpaRepository<EventCandidature, UUID> {

    List<EventCandidature> findByEventId(UUID eventId);

    Optional<EventCandidature> findByParticipantIdAndEventId(String participantId, UUID eventId);

    List<EventCandidature> findByEventIdAndStatus(UUID eventId, CandidatureStatus status);
}

