package com.codearena.module6_event.controller;

import com.codearena.module6_event.dto.CreateEventRequest;
import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.service.EventService;
import com.codearena.module6_event.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final InvitationService invitationService;

    @GetMapping
    public ResponseEntity<?> getEvents(@RequestParam Optional<EventType> type) {
        if (type.isPresent()) {
            List<EventDto> list = eventService.getAllEventsByType(type.get());
            return ResponseEntity.ok(list);
        }
        List<EventDto> list = eventService.getAllEvents();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable UUID id) {
        EventDto dto = eventService.getEventById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getEventStats(@PathVariable UUID id) {
        Map<String, Object> stats = eventService.getEventStats(id);
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody CreateEventRequest dto) {
        EventDto created = eventService.createEvent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable UUID id, @RequestBody CreateEventRequest dto) {
        EventDto updated = eventService.updateEvent(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invite-top10")
    public ResponseEntity<?> inviteTop10(@PathVariable UUID id) {
        int sentCount = invitationService.inviteTop10Players(id);
        return ResponseEntity.ok(sentCount);
    }
}
