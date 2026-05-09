package com.codearena.module6_event;

import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventStatus;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.exception.AlreadyRegisteredException;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventInvitationRepository;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import com.codearena.module6_event.service.CandidatureService;
import com.codearena.module6_event.service.EmailService;
import com.codearena.module6_event.service.ParticipantIdentityService;
import com.codearena.module6_event.service.RegistrationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock EventRegistrationRepository registrationRepository;
    @Mock EventMapper eventMapper;
    @Mock EventInvitationRepository invitationRepository;
    @Mock CandidatureService candidatureService;
    @Mock ParticipantIdentityService participantIdentityService;
    @Mock EmailService emailService;
    
    @InjectMocks RegistrationServiceImpl registrationService;

    @Test
    void register_shouldReturnConfirmed_whenPlacesAvailable() {
        UUID eventId = UUID.randomUUID();
        String participantId = "user-123";
        
        ProgrammingEvent event = new ProgrammingEvent();
        event.setId(eventId);
        event.setType(EventType.OPEN);
        event.setMaxParticipants(50);
        event.setCurrentParticipants(10);
        event.setStatus("UPCOMING");
        
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.of(event));
        when(registrationRepository
            .findByParticipantIdAndEvent_Id(participantId, eventId))
            .thenReturn(Optional.empty());
        
        EventRegistration saved = new EventRegistration();
        saved.setStatus(EventStatus.CONFIRMED);
        when(registrationRepository.save(any()))
            .thenReturn(saved);
        
        RegistrationResponseDTO dto = new RegistrationResponseDTO();
        dto.setStatus(EventStatus.CONFIRMED);
        dto.setParticipantId(participantId);
        when(eventMapper.toRegistrationResponseDTO(saved)).thenReturn(dto);
        when(participantIdentityService.resolveDisplayName(participantId)).thenReturn("Test User");
        
        RegistrationResponseDTO result = 
            registrationService.register(eventId, participantId);
        
        assertEquals("CONFIRMED", result.getStatus().name());
    }

    @Test
    void register_shouldReturnWaitlist_whenEventFull() {
        UUID eventId = UUID.randomUUID();
        String participantId = "user-123";
        
        ProgrammingEvent event = new ProgrammingEvent();
        event.setId(eventId);
        event.setType(EventType.OPEN);
        event.setMaxParticipants(10);
        event.setCurrentParticipants(10);
        
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.of(event));
        when(registrationRepository
            .findByParticipantIdAndEvent_Id(participantId, eventId))
            .thenReturn(Optional.empty());
        
        EventRegistration saved = new EventRegistration();
        saved.setStatus(EventStatus.WAITLIST);
        when(registrationRepository.save(any()))
            .thenReturn(saved);

        RegistrationResponseDTO dto = new RegistrationResponseDTO();
        dto.setStatus(EventStatus.WAITLIST);
        dto.setParticipantId(participantId);
        when(eventMapper.toRegistrationResponseDTO(saved)).thenReturn(dto);
        when(participantIdentityService.resolveDisplayName(participantId)).thenReturn("Test User");
        
        RegistrationResponseDTO result = 
            registrationService.register(eventId, participantId);
        
        assertEquals("WAITLIST", result.getStatus().name());
    }

    @Test
    void register_shouldThrow_whenAlreadyRegistered() {
        UUID eventId = UUID.randomUUID();
        String participantId = "user-123";
        
        ProgrammingEvent event = new ProgrammingEvent();
        event.setType(EventType.OPEN);
        
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.of(event));
        
        EventRegistration existing = new EventRegistration();
        existing.setStatus(EventStatus.CONFIRMED);
        when(registrationRepository
            .findByParticipantIdAndEvent_Id(participantId, eventId))
            .thenReturn(Optional.of(existing));
        
        assertThrows(AlreadyRegisteredException.class,
            () -> registrationService.register(eventId, participantId));
    }
}
