package com.codearena.module6_event.repository;

import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    List<EventRegistration> findByEvent_Id(UUID eventId);

    Optional<EventRegistration> findByParticipantIdAndEvent_Id(String participantId, UUID eventId);

    long countByEvent_IdAndStatus(UUID eventId, EventStatus status);

    Optional<EventRegistration> findFirstByEvent_IdAndStatusOrderByRegisteredAtAsc(
            UUID eventId, EventStatus status);

    List<EventRegistration> findByParticipantId(String participantId);

    List<EventRegistration> findByEvent_IdAndStatus(UUID eventId, EventStatus status);
}
