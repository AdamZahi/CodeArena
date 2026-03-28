package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.CreateEventRequest;
import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventStatus;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.exception.EventNotFoundException;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMapper eventMapper;

    @Override
    @Transactional
    public EventDto createEvent(CreateEventRequest dto) {
        applyCreateDefaults(dto);
        ProgrammingEvent entity = eventMapper.toEntity(dto);
        ProgrammingEvent saved = eventRepository.save(entity);
        return eventMapper.toResponseDTO(saved);
    }

    private void applyCreateDefaults(CreateEventRequest dto) {
        if (dto.getStatus() == null || dto.getStatus().isBlank()) {
            dto.setStatus("UPCOMING");
        }
        if (dto.getType() == null) {
            dto.setType(EventType.OPEN);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(eventMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllEventsByType(EventType type) {
        return eventRepository.findByType(type).stream()
                .map(eventMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
        ProgrammingEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));
        return eventMapper.toResponseDTO(event);
    }

    @Override
    @Transactional
    public EventDto updateEvent(UUID id, CreateEventRequest dto) {
        ProgrammingEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getOrganizerId() != null) {
            event.setOrganizerId(dto.getOrganizerId());
        }
        if (dto.getStartDate() != null) {
            event.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            event.setEndDate(dto.getEndDate());
        }
        if (dto.getMaxParticipants() != null) {
            event.setMaxParticipants(dto.getMaxParticipants());
        }
        if (dto.getType() != null) {
            event.setType(dto.getType());
        }
        if (dto.getCategory() != null) {
            event.setCategory(dto.getCategory());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            event.setStatus(dto.getStatus());
        }
        ProgrammingEvent saved = eventRepository.save(event);
        return eventMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new EventNotFoundException("Event not found: " + id);
        }
        registrationRepository.deleteAll(registrationRepository.findByEvent_Id(id));
        eventRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEventStats(UUID id) {
        ProgrammingEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));
        int max = event.getMaxParticipants() == null ? 0 : event.getMaxParticipants();
        int cur = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();
        long waitlistCount = registrationRepository.countByEvent_IdAndStatus(id, EventStatus.WAITLIST);
        double fillRate = max == 0 ? 0.0 : (cur * 100.0) / max;
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPlaces", max);
        stats.put("occupiedPlaces", cur);
        stats.put("waitlistCount", waitlistCount);
        stats.put("fillRate", fillRate);
        return stats;
    }
}
