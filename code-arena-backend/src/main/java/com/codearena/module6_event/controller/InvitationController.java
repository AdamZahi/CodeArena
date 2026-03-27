package com.codearena.module6_event.controller;

import com.codearena.module6_event.dto.InvitationResponseDTO;
import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/me/invitations")
    public ResponseEntity<?> getMyInvitations(@AuthenticationPrincipal Jwt jwt) {
        String participantId = jwt.getSubject();
        return ResponseEntity.ok(invitationService.getMyInvitations(participantId));
    }

    @PutMapping("/{id}/invitation/accept")
    public ResponseEntity<RegistrationResponseDTO> acceptInvitation(
            @PathVariable("id") UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {
        String participantId = jwt.getSubject();
        return ResponseEntity.ok(invitationService.acceptInvitation(eventId, participantId));
    }

    @PutMapping("/{id}/invitation/decline")
    public ResponseEntity<InvitationResponseDTO> declineInvitation(
            @PathVariable("id") UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {
        String participantId = jwt.getSubject();
        return ResponseEntity.ok(invitationService.declineInvitation(eventId, participantId));
    }

    private String getCurrentUserId(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getSubject();
    }
}

