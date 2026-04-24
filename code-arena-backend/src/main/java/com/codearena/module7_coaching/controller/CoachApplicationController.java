package com.codearena.module7_coaching.controller;

import com.codearena.module7_coaching.dto.CoachApplicationDto;
import com.codearena.module7_coaching.entity.CoachApplication;
import com.codearena.module7_coaching.entity.Coach;
import com.codearena.module7_coaching.repository.CoachApplicationRepository;
import com.codearena.module7_coaching.repository.CoachRepository;
import com.codearena.module7_coaching.service.EmailService;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/coaching/applications")
@RequiredArgsConstructor
public class CoachApplicationController {

    private final CoachApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final EmailService emailService;

    // ═══════ SUBMIT APPLICATION (any authenticated user) ═══════
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> submitApplication(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CoachApplicationDto dto) {
        try {
            String userId = jwt.getSubject();

            // Check if user already has a pending application
            if (applicationRepository.existsByUserIdAndStatus(userId, CoachApplication.ApplicationStatus.PENDING)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "You already have a pending application. Please wait for admin review."));
            }

            // Check if user is already a coach
            Optional<User> userOpt = userRepository.findByKeycloakId(userId);
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.COACH) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "You are already a coach."));
            }

            String email = jwt.getClaimAsString("email");
            String name = dto.getApplicantName();
            if (name == null || name.isBlank()) {
                name = jwt.getClaimAsString("name");
                if (name == null) name = "Unknown";
            }

            CoachApplication application = CoachApplication.builder()
                    .userId(userId)
                    .applicantName(name)
                    .applicantEmail(email != null ? email : (dto.getApplicantEmail() != null ? dto.getApplicantEmail() : ""))
                    .cvContent(dto.getCvContent())
                    .cvFileBase64(dto.getCvFileBase64())
                    .cvFileName(dto.getCvFileName())
                    .status(CoachApplication.ApplicationStatus.PENDING)
                    .build();

            application = applicationRepository.save(application);
            log.info("Coach application submitted by {} ({})", name, userId);
            log.info("Application has PDF file: {}, fileName: {}", 
                     application.getCvFileBase64() != null && !application.getCvFileBase64().isEmpty(),
                     application.getCvFileName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Application submitted successfully. Pending admin review.",
                    "data", toDto(application)));
        } catch (Exception e) {
            log.error("Error submitting coach application", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    // ═══════ GET MY APPLICATION STATUS (authenticated user) ═══════
    @GetMapping("/my-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyApplicationStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Optional<CoachApplication> appOpt = applicationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
        if (appOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "data", Map.of("status", "NONE")));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", toDto(appOpt.get())));
    }

    // ═══════ GET ALL APPLICATIONS (admin only) ═══════
    @GetMapping
    @PreAuthorize("@coachingSecurity.isAdmin(principal)")
    public ResponseEntity<Map<String, Object>> getAllApplications(@AuthenticationPrincipal Jwt jwt) {
        List<CoachApplicationDto> applications = applicationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", applications));
    }

    // ═══════ GET PENDING APPLICATIONS (admin only) ═══════
    @GetMapping("/pending")
    @PreAuthorize("@coachingSecurity.isAdmin(principal)")
    public ResponseEntity<Map<String, Object>> getPendingApplications(@AuthenticationPrincipal Jwt jwt) {
        List<CoachApplicationDto> pending = applicationRepository
                .findByStatus(CoachApplication.ApplicationStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", pending));
    }

    // ═══════ APPROVE APPLICATION (admin only) ═══════
    @PostMapping("/{id}/approve")
    @PreAuthorize("@coachingSecurity.isAdmin(principal)")
    public ResponseEntity<Map<String, Object>> approveApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "id") UUID id,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            CoachApplication application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            if (application.getStatus() != CoachApplication.ApplicationStatus.PENDING) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "This application has already been " + application.getStatus().name().toLowerCase()));
            }

            // Update application status
            application.setStatus(CoachApplication.ApplicationStatus.APPROVED);
            application.setReviewedAt(LocalDateTime.now());
            if (body != null && body.containsKey("adminNote")) {
                application.setAdminNote(body.get("adminNote"));
            }
            applicationRepository.save(application);

            // Promote user to COACH role
            Optional<User> userOpt = userRepository.findByKeycloakId(application.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setRole(Role.COACH);
                userRepository.save(user);

                // Create coach profile if doesn't exist
                if (!coachRepository.existsByUserId(application.getUserId())) {
                    coachRepository.save(Coach.builder()
                            .userId(application.getUserId())
                            .bio("Approved coach on Code Arena. " + application.getApplicantName())
                            .specializations(new ArrayList<>(List.of("GENERAL")))
                            .rating(0.0)
                            .totalSessions(0)
                            .build());
                }

                // Send notification email
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    try {
                        emailService.sendApplicationDecisionEmail(
                                application.getApplicantName(),
                                user.getEmail(),
                                true,
                                application.getAdminNote());
                    } catch (Exception e) {
                        log.warn("Could not send approval email: {}", e.getMessage());
                    }
                }
            }

            log.info("Admin approved coach application for {} ({})", application.getApplicantName(), application.getUserId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Application approved. " + application.getApplicantName() + " is now a coach.",
                    "data", toDto(application)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    // ═══════ REJECT APPLICATION (admin only) ═══════
    @PostMapping("/{id}/reject")
    @PreAuthorize("@coachingSecurity.isAdmin(principal)")
    public ResponseEntity<Map<String, Object>> rejectApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable(name = "id") UUID id,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            CoachApplication application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            if (application.getStatus() != CoachApplication.ApplicationStatus.PENDING) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "This application has already been " + application.getStatus().name().toLowerCase()));
            }

            application.setStatus(CoachApplication.ApplicationStatus.REJECTED);
            application.setReviewedAt(LocalDateTime.now());
            if (body != null && body.containsKey("adminNote")) {
                application.setAdminNote(body.get("adminNote"));
            }
            applicationRepository.save(application);

            // Send rejection email
            Optional<User> userOpt = userRepository.findByKeycloakId(application.getUserId());
            if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
                try {
                    emailService.sendApplicationDecisionEmail(
                            application.getApplicantName(),
                            userOpt.get().getEmail(),
                            false,
                            application.getAdminNote());
                } catch (Exception e) {
                    log.warn("Could not send rejection email: {}", e.getMessage());
                }
            }

            log.info("Admin rejected coach application for {}", application.getApplicantName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Application rejected for " + application.getApplicantName(),
                    "data", toDto(application)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    // ═══════ HELPERS ═══════

    private CoachApplicationDto toDto(CoachApplication app) {
        return CoachApplicationDto.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .applicantName(app.getApplicantName())
                .applicantEmail(app.getApplicantEmail())
                .cvContent(app.getCvContent())
                .cvFileBase64(app.getCvFileBase64())
                .cvFileName(app.getCvFileName())
                .status(app.getStatus().name())
                .adminNote(app.getAdminNote())
                .createdAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .build();
    }
}
