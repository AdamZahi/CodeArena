package com.codearena.module6_event.controller;

import com.codearena.module6_event.dto.CreateEventRequest;
import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.service.EventService;
import com.codearena.module6_event.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final InvitationService invitationService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<EventDto>> getEvents(@RequestParam(name = "type", required = false) EventType type) {
        if (type != null) {
            return ResponseEntity.ok(eventService.getAllEventsByType(type));
        }
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getEventStats(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(eventService.getEventStats(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EventDto> createEvent(@RequestBody CreateEventRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EventDto> updateEvent(@PathVariable("id") UUID id,
            @RequestBody CreateEventRequest dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/invite-top10")
    public ResponseEntity<Integer> inviteTop10(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(invitationService.inviteTop10Players(id));
    }
}
