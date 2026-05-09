package com.codearena.module6_event;

import com.codearena.module6_event.entity.EventCandidature;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.exception.AlreadySubmittedCandidatureException;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventCandidatureRepository;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import com.codearena.module6_event.service.CandidatureServiceImpl;
import com.codearena.module6_event.service.ParticipantIdentityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock EventCandidatureRepository candidatureRepository;
    @Mock EventRegistrationRepository registrationRepository;
    @Mock EventMapper eventMapper;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ParticipantIdentityService participantIdentityService;
    
    @InjectMocks CandidatureServiceImpl candidatureService;

    @Test
    void submitCandidature_shouldThrow_whenAlreadySubmitted() {
        UUID eventId = UUID.randomUUID();
        String participantId = "user-123";
        
        ProgrammingEvent event = new ProgrammingEvent();
        event.setType(EventType.EXCLUSIVE);
        
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.of(event));
        when(candidatureRepository
            .findByParticipantIdAndEventId(participantId, eventId))
            .thenReturn(Optional.of(new EventCandidature()));
        
        assertThrows(AlreadySubmittedCandidatureException.class,
            () -> candidatureService.submitCandidature(
                eventId, participantId, "motivation"));
    }

    @Test
    void submitCandidature_shouldThrow_whenEventIsOpen() {
        UUID eventId = UUID.randomUUID();
        
        ProgrammingEvent event = new ProgrammingEvent();
        event.setType(EventType.OPEN);
        
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.of(event));
        
        assertThrows(RuntimeException.class,
            () -> candidatureService.submitCandidature(
                eventId, "user-123", "motivation"));
    }
}
