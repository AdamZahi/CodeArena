package com.codearena.module6_event.controller;

import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/{id}/register")
    public ResponseEntity<?> register(
            @PathVariable("id") UUID eventId,
            @RequestParam(name = "participantId") String participantId) {
        RegistrationResponseDTO dto = registrationService.register(eventId, participantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<?> cancelRegistration(
            @PathVariable("id") UUID eventId,
            @RequestParam(name = "participantId") String participantId) {
        registrationService.cancelRegistration(eventId, participantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<?> getEventParticipants(@PathVariable("id") UUID eventId) {
        List<RegistrationResponseDTO> list = registrationService.getEventParticipants(eventId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me/registrations")
    public ResponseEntity<?> getMyRegistrations(@RequestParam(name = "participantId") String participantId) {
        List<RegistrationResponseDTO> list = registrationService.getMyRegistrations(participantId);
        return ResponseEntity.ok(list);
    }
}
