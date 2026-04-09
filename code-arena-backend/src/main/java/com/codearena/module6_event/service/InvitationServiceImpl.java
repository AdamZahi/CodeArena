package com.codearena.module6_event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codearena.module6_event.dto.InvitationResponseDTO;
import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.entity.EventInvitation;
import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventStatus;
import com.codearena.module6_event.enums.InvitationStatus;
import com.codearena.module6_event.exception.EventNotFoundException;
import com.codearena.module6_event.exception.InvitationNotFoundException;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventInvitationRepository;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final EventInvitationRepository invitationRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMapper eventMapper;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Qualifier("eventEmailService")
    private final EmailService emailService;

    @Override
    @Transactional
    public int inviteTop10Players(UUID eventId) {
        ProgrammingEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        List<String> top10 = List.of(
                "google-oauth2|115542530890235890362",
                "google-oauth2|115948396210140607346",
                "google-oauth2|115021199274267292708",
                "google-oauth2|112255744825732358525",
                "google-oauth2|100499137846569783005",
                "google-oauth2|108378133921738621575",
                "google-oauth2|108133574113488267379",
                "google-oauth2|110630587307020708631 7",
                "github|134744963",
                "auth0|69c5a6978245aa1f8bde6caa");

        List<EventInvitation> toSave = new ArrayList<>();
        for (String participantId : top10) {
            Optional<EventInvitation> existing = invitationRepository.findFirstByEventIdAndParticipantId(eventId,
                    participantId);
            if (existing.isPresent()) {
                continue;
            }

            EventInvitation invitation = EventInvitation.builder()
                    .eventId(eventId)
                    .participantId(participantId)
                    .status(InvitationStatus.PENDING)
                    .build();
            invitation = invitationRepository.save(invitation);
            toSave.add(invitation);

            try {
                String testEmail = "codearenapi@gmail.com";
                emailService.sendInvitationEmail(testEmail, event.getTitle(),
                        event.getStartDate() != null ? event.getStartDate().toString() : "",
                        event.getLocation() != null ? event.getLocation() : "");
                log.info("Invitation email sent to {}", participantId);
            } catch (Exception e) {
                log.error("Failed to send invitation email to {}", participantId, e);
            }
        }

        return toSave.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponseDTO> getMyInvitations(String participantId) {
        return invitationRepository.findByParticipantId(participantId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public RegistrationResponseDTO acceptInvitation(UUID eventId, String participantId) {
        EventInvitation invitation = invitationRepository
                .findFirstByEventIdAndParticipantId(eventId, participantId)
                .orElseThrow(() -> new InvitationNotFoundException(
                        "Invitation not found for eventId=" + eventId + " participantId=" + participantId));

        ProgrammingEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        Optional<EventRegistration> existingRegistration = registrationRepository
                .findByParticipantIdAndEvent_Id(participantId, eventId);

        int current = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();
        String qrCode = generateQRCode(eventId, participantId);

        EventRegistration registrationToReturn;
        if (existingRegistration.isPresent()) {
            EventRegistration existing = existingRegistration.get();
            if (existing.getStatus() != EventStatus.CANCELLED) {
                if (existing.getQrCode() == null || existing.getQrCode().isBlank()) {
                    existing.setQrCode(qrCode);
                    registrationRepository.save(existing);
                }
                return eventMapper.toRegistrationResponseDTO(existing);
            }

            existing.setStatus(EventStatus.CONFIRMED);
            existing.setQrCode(qrCode);
            existing.setInvited(false);
            existing.setInvitationCode(null);
            registrationToReturn = existing;
        } else {
            registrationToReturn = EventRegistration.builder()
                    .participantId(participantId)
                    .event(event)
                    .status(EventStatus.CONFIRMED)
                    .qrCode(qrCode)
                    .invited(false)
                    .invitationCode(null)
                    .build();
        }

        event.setCurrentParticipants(current + 1);
        eventRepository.save(event);
        EventRegistration saved = registrationRepository.save(registrationToReturn);

        // ── WEBSOCKET NOTIFICATION ─────────────────
        messagingTemplate.convertAndSend(
            "/topic/admin/invitations",
            new java.util.HashMap<String, Object>() {{
                put("type", "INVITATION_ACCEPTED");
                put("eventId", event.getId().toString());
                put("participantId", participantId);
                put("message", "Player accepted VIP invitation for event: " + event.getTitle());
            }}
        );

        return eventMapper.toRegistrationResponseDTO(saved);
    }

    @Override
    @Transactional
    public InvitationResponseDTO declineInvitation(UUID eventId, String participantId) {
        EventInvitation invitation = invitationRepository
                .findFirstByEventIdAndParticipantId(eventId, participantId)
                .orElseThrow(() -> new InvitationNotFoundException(
                        "Invitation not found for eventId=" + eventId + " participantId=" + participantId));

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        EventInvitation saved = invitationRepository.save(invitation);

        return toResponseDTO(saved);
    }

    private InvitationResponseDTO toResponseDTO(EventInvitation invitation) {
        return InvitationResponseDTO.builder()
                .id(invitation.getId())
                .eventId(invitation.getEventId())
                .participantId(invitation.getParticipantId())
                .status(invitation.getStatus())
                .sentAt(invitation.getSentAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }


    private String generateQRCode(UUID eventId, String participantId) {
        return "CODEARENA|EVENT:" + eventId
                + "|USER:" + participantId
                + "|TOKEN:" + UUID.randomUUID();
    }
}
