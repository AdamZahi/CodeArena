package com.codearena.module6_event;

import com.codearena.module6_event.dto.CreateEventRequest;
import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.exception.EventNotFoundException;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import com.codearena.module6_event.service.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock EventRegistrationRepository registrationRepository;
    @Mock EventMapper eventMapper;
    @InjectMocks EventServiceImpl eventService;

    @Test
    void createEvent_shouldSaveAndReturnDto() {
        // Given
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Test Hackathon");
        request.setType(EventType.OPEN);
        request.setMaxParticipants(50);
        
        ProgrammingEvent entity = new ProgrammingEvent();
        EventDto dto = new EventDto();
        dto.setTitle("Test Hackathon");
        
        when(eventMapper.toEntity(request)).thenReturn(entity);
        when(eventRepository.save(entity)).thenReturn(entity);
        when(eventMapper.toResponseDTO(entity)).thenReturn(dto);
        
        // When
        EventDto result = eventService.createEvent(request);
        
        // Then
        assertNotNull(result);
        assertEquals("Test Hackathon", result.getTitle());
        verify(eventRepository, times(1)).save(entity);
    }

    @Test
    void getEventById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id))
            .thenReturn(Optional.empty());
        
        assertThrows(EventNotFoundException.class,
            () -> eventService.getEventById(id));
    }

    @Test
    void deleteEvent_shouldDeleteRegistrationsFirst() {
        UUID id = UUID.randomUUID();
        when(eventRepository.existsById(id)).thenReturn(true);
        when(registrationRepository.findByEvent_Id(id))
            .thenReturn(List.of());
        
        eventService.deleteEvent(id);
        
        verify(registrationRepository).deleteAll(any());
        verify(eventRepository).deleteById(id);
    }

    @Test
    void getEventStats_shouldCalculateFillRate() {
        UUID id = UUID.randomUUID();
        ProgrammingEvent event = new ProgrammingEvent();
        event.setMaxParticipants(100);
        event.setCurrentParticipants(50);
        
        when(eventRepository.findById(id))
            .thenReturn(Optional.of(event));
        when(registrationRepository
            .countByEvent_IdAndStatus(any(), any()))
            .thenReturn(5L);
        
        Map<String, Object> stats = eventService.getEventStats(id);
        
        assertEquals(100, stats.get("totalPlaces"));
        assertEquals(50, stats.get("occupiedPlaces"));
        assertEquals(50.0, stats.get("fillRate"));
    }
}
