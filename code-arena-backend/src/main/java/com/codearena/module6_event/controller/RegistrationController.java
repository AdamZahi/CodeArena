package com.codearena.module6_event.controller;

import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/register")
    public ResponseEntity<RegistrationResponseDTO> register(
            @PathVariable("id") UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {
        String participantId = jwt.getSubject();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(eventId, participantId));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}/register")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable("id") UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {
        String participantId = jwt.getSubject();
        registrationService.cancelRegistration(eventId, participantId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<RegistrationResponseDTO>> getEventParticipants(
            @PathVariable("id") UUID eventId) {
        return ResponseEntity.ok(registrationService.getEventParticipants(eventId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/registrations")
    public ResponseEntity<List<RegistrationResponseDTO>> getMyRegistrations(
            @AuthenticationPrincipal Jwt jwt) {
        String participantId = jwt.getSubject();
        return ResponseEntity.ok(registrationService.getMyRegistrations(participantId));
    }
}
