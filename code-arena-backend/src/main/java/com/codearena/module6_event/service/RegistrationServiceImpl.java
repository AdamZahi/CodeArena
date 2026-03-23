package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.dto.CandidatureResponseDTO;
import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.EventInvitation;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventStatus;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.enums.InvitationStatus;
import com.codearena.module6_event.exception.AlreadyRegisteredException;
import com.codearena.module6_event.exception.EventFullException;
import com.codearena.module6_event.exception.EventNotFoundException;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventInvitationRepository;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMapper eventMapper;
    private final EventInvitationRepository invitationRepository;
    private final CandidatureService candidatureService;

    @Override
    @Transactional
    public RegistrationResponseDTO register(UUID eventId, String participantId) {
        ProgrammingEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        Optional<EventRegistration> existing = registrationRepository.findByParticipantIdAndEvent_Id(
                participantId, eventId);
        if (existing.isPresent() && existing.get().getStatus() != EventStatus.CANCELLED) {
            throw new AlreadyRegisteredException("Participant already registered for this event");
        }

        EventRegistration registration = null;
        switch (event.getType()) {
            case OPEN -> registration = registerOpen(event, participantId, existing);
            case EXCLUSIVE -> {
                Optional<EventInvitation> invitation = invitationRepository
                        .findByEventIdAndParticipantId(eventId, participantId);
                boolean accepted = invitation.isPresent() && invitation.get().getStatus() == InvitationStatus.ACCEPTED;

                if (accepted) {
                    int current = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();
                    String qrCode = generateQRCode(event.getId(), participantId);

                    EventRegistration registrationToSave;
                    if (existing.isPresent()) {
                        EventRegistration reg = existing.get();
                        reg.setStatus(EventStatus.CONFIRMED);
                        reg.setQrCode(qrCode);
                        reg.setInvited(false);
                        reg.setInvitationCode(null);
                        registrationToSave = reg;
                    } else {
                        registrationToSave = EventRegistration.builder()
                                .participantId(participantId)
                                .event(event)
                                .status(EventStatus.CONFIRMED)
                                .qrCode(qrCode)
                                .invited(false)
                                .build();
                    }

                    event.setCurrentParticipants(current + 1);
                    eventRepository.save(event);
                    registration = registrationToSave;
                } else {
                    CandidatureResponseDTO candidature = candidatureService
                            .submitCandidature(eventId, participantId, "");
                    return RegistrationResponseDTO.builder()
                            .id(candidature.getId())
                            .participantId(participantId)
                            .eventId(eventId)
                            .status(EventStatus.WAITLIST)
                            .qrCode(null)
                            .registeredAt(candidature.getAppliedAt())
                            .build();
                }
            }
        }

        if (registration == null) {
            throw new EventFullException("Registration failed");
        }

        EventRegistration saved = registrationRepository.save(registration);
        return eventMapper.toRegistrationResponseDTO(saved);
    }

    private EventRegistration registerOpen(
            ProgrammingEvent event,
            String participantId,
            Optional<EventRegistration> existingCancelled) {

        int max = event.getMaxParticipants() == null ? 0 : event.getMaxParticipants();
        int current = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();

        EventStatus status;
        String qrCode = null;

        if (current < max) {
            status = EventStatus.CONFIRMED;
            qrCode = generateQRCode(event.getId(), participantId);
            event.setCurrentParticipants(current + 1);
            eventRepository.save(event);
        } else {
            status = EventStatus.WAITLIST;
        }

        if (existingCancelled.isPresent()) {
            EventRegistration reg = existingCancelled.get();
            reg.setStatus(status);
            reg.setQrCode(qrCode);
            reg.setInvited(false);
            reg.setInvitationCode(null);
            return reg;
        }

        return EventRegistration.builder()
                .participantId(participantId)
                .event(event)
                .status(status)
                .qrCode(qrCode)
                .invited(false)
                .build();
    }

    @Override
    @Transactional
    public void cancelRegistration(UUID eventId, String participantId) {
        EventRegistration registration = registrationRepository
                .findByParticipantIdAndEvent_Id(participantId, eventId)
                .orElseThrow(() -> new EventNotFoundException("Registration not found for participant and event"));

        if (registration.getStatus() == EventStatus.CANCELLED) {
            throw new EventNotFoundException("Registration not found for participant and event");
        }

        ProgrammingEvent event = registration.getEvent();
        if (registration.getStatus() == EventStatus.CONFIRMED) {
            int current = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();
            event.setCurrentParticipants(Math.max(0, current - 1));
            eventRepository.save(event);
        }

        registration.setStatus(EventStatus.CANCELLED);
        registrationRepository.save(registration);

        promoteFromWaitlist(eventId);
    }

    private void promoteFromWaitlist(UUID eventId) {
        ProgrammingEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        int max = event.getMaxParticipants() == null ? 0 : event.getMaxParticipants();
        int current = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();
        if (max <= 0 || current >= max) {
            return;
        }

        Optional<EventRegistration> next = registrationRepository
                .findFirstByEvent_IdAndStatusOrderByRegisteredAtAsc(eventId, EventStatus.WAITLIST);
        if (next.isEmpty()) {
            return;
        }

        EventRegistration promoted = next.get();
        promoted.setStatus(EventStatus.CONFIRMED);
        promoted.setQrCode(generateQRCode(eventId, promoted.getParticipantId()));
        event.setCurrentParticipants(current + 1);
        eventRepository.save(event);
        registrationRepository.save(promoted);
    }

    private String generateQRCode(UUID eventId, String participantId) {
        return "CODEARENA|EVENT:" + eventId
                + "|USER:" + participantId
                + "|TOKEN:" + UUID.randomUUID();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponseDTO> getEventParticipants(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException("Event not found: " + eventId);
        }
        return registrationRepository.findByEvent_Id(eventId).stream()
                .map(eventMapper::toRegistrationResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponseDTO> getMyRegistrations(String participantId) {
        return registrationRepository.findByParticipantId(participantId).stream()
                .map(eventMapper::toRegistrationResponseDTO)
                .toList();
    }
}
