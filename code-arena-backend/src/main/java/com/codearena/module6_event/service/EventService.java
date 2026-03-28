package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.CreateEventRequest;
import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.enums.EventType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EventService {

    EventDto createEvent(CreateEventRequest dto);

    List<EventDto> getAllEvents();

    List<EventDto> getAllEventsByType(EventType type);

    EventDto getEventById(UUID id);

    EventDto updateEvent(UUID id, CreateEventRequest dto);

    void deleteEvent(UUID id);

    Map<String, Object> getEventStats(UUID id);
}
