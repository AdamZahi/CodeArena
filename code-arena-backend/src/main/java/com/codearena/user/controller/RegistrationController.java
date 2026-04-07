package com.codearena.user.controller;

import com.codearena.config.Auth0Config;
import com.codearena.module7_coaching.entity.Coach;
import com.codearena.module7_coaching.repository.CoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.codearena.user.entity.AuthProvider;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;

import java.util.Arrays;
import java.util.Map;

/**
 * Public registration endpoint.
 * Uses Auth0's public /dbconnections/signup endpoint — does NOT require the
 * Management API / client_credentials grant (avoids "unauthorized_client" error
 * when the registered Auth0 app is a SPA type).
 *
 * Role assignment is stored locally in our own `coaches` table.
 * An Auth0 Action (post-login) can also be set up to read roles from our API.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class RegistrationController {

    private final Auth0Config auth0Config;
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Request body:
     * {
     *   "firstName": "Hazem",
     *   "lastName":  "Mtir",
     *   "email":     "hazem@example.com",
     *   "password":  "Hazem007*",
     *   "role":      "PARTICIPANT"  // or "COACH"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String firstName = body.getOrDefault("firstName", "").trim();
        String lastName  = body.getOrDefault("lastName",  "").trim();
        String email     = body.get("email");
        String password  = body.get("password");
        String role      = body.getOrDefault("role", "PARTICIPANT").toUpperCase();

        // ── Validation ─────────────────────────────────────────────
        if (email == null || email.isBlank()) {
            return badRequest("Email is required.");
        }
        if (password == null || password.isBlank()) {
            return badRequest("Password is required.");
        }
        if (!role.equals("COACH") && !role.equals("PARTICIPANT")) {
            return badRequest("Role must be COACH or PARTICIPANT.");
        }

        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isBlank()) fullName = email.split("@")[0];

        // ── Step 1: Create user via Auth0 public signup endpoint ───
        // This endpoint does NOT require the Management API / client_credentials grant.
        String signupUrl = String.format("https://%s/dbconnections/signup", auth0Config.getDomain());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> signupBody = Map.of(
            "client_id",  auth0Config.getClientId(),
            "email",      email,
            "password",   password,
            "connection", "Username-Password-Authentication",
            "name",       fullName,
            "user_metadata", Map.of("role", role)
        );

        String auth0UserId = null;
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                signupUrl, HttpMethod.POST,
                new HttpEntity<>(signupBody, headers),
                Map.class
            );
            if (resp.getBody() != null) {
                // Auth0 returns _id for /dbconnections/signup
                Object rawId = resp.getBody().get("_id");
                if (rawId != null) {
                    // Convert short _id to full sub format: auth0|<_id>
                    auth0UserId = "auth0|" + rawId.toString();
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("Auth0 signup failed for {}: {} - {}", email, e.getStatusCode(), e.getResponseBodyAsString());
            String errorBody = e.getResponseBodyAsString();
            if (errorBody.contains("user already exists") || errorBody.contains("already_exists")) {
                return conflict("An account with this email already exists. Please sign in.");
            }
            if (errorBody.contains("PasswordStrengthError") || errorBody.contains("password")) {
                return badRequest("Password is too weak. Use at least 8 characters, one uppercase, one number, and one symbol (e.g. Hazem007*).");
            }
            return serverError("Registration failed: " + errorBody);
        } catch (Exception e) {
            log.error("Registration error for {}: {}", email, e.getMessage());
            return serverError("Registration failed. Please try again.");
        }

        // ── Step 2: Create local user immediately ───────────────────
        if (auth0UserId != null) {
            final String userId = auth0UserId;
            
            // 2a. Save into users table
            if (userRepository.findByKeycloakId(userId).isEmpty()) {
                userRepository.save(User.builder()
                    .keycloakId(userId)
                    .email(email)
                    .firstName(firstName.isBlank() ? null : firstName)
                    .lastName(lastName.isBlank() ? null : lastName)
                    .role("COACH".equals(role) ? Role.COACH : Role.PARTICIPANT)
                    .authProvider(AuthProvider.LOCAL)
                    .isActive(true)
                    .build());
                log.info("Created local user profile for {} ({}) as {}", email, userId, role);
            }

            // 2b. If COACH, create local coach profile in module7
            if ("COACH".equals(role)) {
                if (coachRepository.findByUserId(userId).isEmpty()) {
                    coachRepository.save(Coach.builder()
                        .userId(userId)
                        .bio("Hi! I'm " + fullName + " — a new coach on Code Arena. Update your bio in your profile.")
                        .specializations(Arrays.asList("JAVA"))
                        .rating(0.0)
                        .totalSessions(0)
                        .build());
                    log.info("Created local coach profile for {} ({})", email, userId);
                }
            }
        }

        log.info("Registered new user: {} with role {} (auth0Id: {})", email, role, auth0UserId);
        return ResponseEntity.ok(Map.of(
            "message", "Account created successfully! You can now log in.",
            "email",   email,
            "role",    role
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> badRequest(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    private ResponseEntity<Map<String, Object>> conflict(String msg) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
    }

    private ResponseEntity<Map<String, Object>> serverError(String msg) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
    }
}
