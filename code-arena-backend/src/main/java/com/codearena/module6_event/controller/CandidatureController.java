package com.codearena.module6_event.controller;

import com.codearena.module6_event.dto.CandidatureRequestDTO;
import com.codearena.module6_event.dto.CandidatureResponseDTO;
import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.service.CandidatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/candidature")
    public ResponseEntity<CandidatureResponseDTO> submitCandidature(
            @PathVariable("id") UUID eventId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CandidatureRequestDTO dto) {
        String participantId = jwt.getSubject();
        CandidatureResponseDTO created =
                candidatureService.submitCandidature(eventId, participantId, dto.getMotivation());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/candidatures")
    public ResponseEntity<List<CandidatureResponseDTO>> getCandidaturesByEvent(
            @PathVariable("id") UUID eventId) {
        return ResponseEntity.ok(candidatureService.getCandidaturesByEvent(eventId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/candidature/{candidatureId}/accept")
    public ResponseEntity<RegistrationResponseDTO> acceptCandidature(
            @PathVariable("candidatureId") UUID candidatureId) {
        return ResponseEntity.ok(candidatureService.acceptCandidature(candidatureId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/candidature/{candidatureId}/reject")
    public ResponseEntity<Void> rejectCandidature(
            @PathVariable("candidatureId") UUID candidatureId) {
        candidatureService.rejectCandidature(candidatureId);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getSubject();
    }
}

