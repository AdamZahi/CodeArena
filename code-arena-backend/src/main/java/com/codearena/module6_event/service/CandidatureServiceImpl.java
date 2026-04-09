package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.CandidatureResponseDTO;
import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.entity.EventCandidature;
import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.CandidatureStatus;
import com.codearena.module6_event.enums.EventStatus;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.exception.AlreadySubmittedCandidatureException;
import com.codearena.module6_event.exception.CandidatureNotFoundException;
import com.codearena.module6_event.exception.EventNotFoundException;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventCandidatureRepository;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidatureServiceImpl implements CandidatureService {

    private final EventRepository eventRepository;
    private final EventCandidatureRepository candidatureRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMapper eventMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipantIdentityService participantIdentityService;

    @Override
    @Transactional
    public CandidatureResponseDTO submitCandidature(UUID eventId, String participantId, String motivation) {
        ProgrammingEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        if (event.getType() != EventType.EXCLUSIVE) {
            throw new UnsupportedOperationException("Candidature only allowed for EXCLUSIVE events");
        }

        Optional<EventCandidature> existing = candidatureRepository
                .findByParticipantIdAndEventId(participantId, eventId);

        if (existing.isPresent()) {
            throw new AlreadySubmittedCandidatureException(
                    "Candidature already submitted for this event");
        }

        EventCandidature candidature = EventCandidature.builder()
                .eventId(eventId)
                .participantId(participantId)
                .motivation(motivation)
                .status(CandidatureStatus.PENDING)
                .build();

        EventCandidature saved = candidatureRepository.save(candidature);

        // ── WEBSOCKET NOTIFICATION ─────────────────
        messagingTemplate.convertAndSend(
            "/topic/admin/candidatures",
            new java.util.HashMap<String, Object>() {{
                put("type", "NEW_CANDIDATURE");
                put("eventId", event.getId().toString());
                put("eventTitle", event.getTitle());
                put("participantId", participantId);
                put("message", "New candidature submitted for event: " + event.getTitle());
            }}
        );

        return toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getCandidaturesByEvent(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException("Event not found: " + eventId);
        }
        return candidatureRepository.findByEventId(eventId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public RegistrationResponseDTO acceptCandidature(UUID candidatureId) {
        EventCandidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new CandidatureNotFoundException(
                        "Candidature not found: " + candidatureId));

        candidature.setStatus(CandidatureStatus.ACCEPTED);
        candidature.setReviewedAt(LocalDateTime.now());
        candidatureRepository.save(candidature);

        UUID eventId = candidature.getEventId();
        String participantId = candidature.getParticipantId();

        ProgrammingEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        int current = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();
        String qrCode = generateQRCode(eventId, participantId);

        Optional<EventRegistration> existingRegistration =
                registrationRepository.findByParticipantIdAndEvent_Id(participantId, eventId);

        EventRegistration registrationToSave;
        boolean needsIncrement = true;

        if (existingRegistration.isPresent()) {
            EventRegistration existing = existingRegistration.get();
            if (existing.getStatus() != EventStatus.CANCELLED) {
                needsIncrement = false;
                existing.setStatus(EventStatus.CONFIRMED);
                if (existing.getQrCode() == null) {
                    existing.setQrCode(qrCode);
                }
                existing.setInvited(false);
                existing.setInvitationCode(null);
                registrationToSave = existing;
            } else {
                existing.setStatus(EventStatus.CONFIRMED);
                existing.setQrCode(qrCode);
                existing.setInvited(false);
                existing.setInvitationCode(null);
                registrationToSave = existing;
            }
        } else {
            registrationToSave = EventRegistration.builder()
                    .participantId(participantId)
                    .event(event)
                    .status(EventStatus.CONFIRMED)
                    .qrCode(qrCode)
                    .invited(false)
                    .invitationCode(null)
                    .build();
        }

        if (needsIncrement) {
            event.setCurrentParticipants(current + 1);
            eventRepository.save(event);
        }
        EventRegistration saved = registrationRepository.save(registrationToSave);
        return eventMapper.toRegistrationResponseDTO(saved);
    }

    @Override
    @Transactional
    public void rejectCandidature(UUID candidatureId) {
        EventCandidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new CandidatureNotFoundException(
                        "Candidature not found: " + candidatureId));

        candidature.setStatus(CandidatureStatus.REJECTED);
        candidature.setReviewedAt(LocalDateTime.now());
        candidatureRepository.save(candidature);
    }

    private CandidatureResponseDTO toResponseDTO(EventCandidature candidature) {
        return CandidatureResponseDTO.builder()
                .id(candidature.getId())
                .eventId(candidature.getEventId())
                .participantId(candidature.getParticipantId())
                .participantName(participantIdentityService.resolveDisplayName(candidature.getParticipantId()))
                .motivation(candidature.getMotivation())
                .status(candidature.getStatus())
                .appliedAt(candidature.getAppliedAt())
                .reviewedAt(candidature.getReviewedAt())
                .build();
    }

    private String generateQRCode(UUID eventId, String participantId) {
        return "CODEARENA|EVENT:" + eventId
                + "|USER:" + participantId
                + "|TOKEN:" + UUID.randomUUID();
    }
}

