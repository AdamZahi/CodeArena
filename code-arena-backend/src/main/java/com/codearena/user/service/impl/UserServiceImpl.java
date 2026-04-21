/*
 * MERGED: UserServiceImpl.java
 * Base: TARGET (258 lines) — guest anonymous fallback, chooseBestDisplayName,
 *       nickname resolution, DataIntegrityViolationException handling
 * Added from SOURCE:
 *   - CoachRepository dependency + coach fallback in resolveRole()
 *   - @Value ghost admin email config + elevation logic in resolveRole()
 *   - try/catch around updateUserRole() in updateRole()
 */
package com.codearena.user.service.impl;

import com.codearena.module7_coaching.repository.CoachRepository;
import com.codearena.user.dto.ProfileUpdateDTO;
import com.codearena.user.dto.UserResponseDTO;
import com.codearena.user.entity.AuthProvider;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.mapper.UserMapper;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Auth0ManagementService auth0ManagementService;
    private final CoachRepository coachRepository; // Added from SOURCE

    @Value("${application.admin.email:}") // Added from SOURCE — ghost admin
    private String adminEmail;

    @Override
    public Page<UserResponseDTO> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    public UserResponseDTO getCurrentUser() {
        // TARGET: graceful anonymous fallback
        Jwt jwt = getCurrentJwtOrNull();
        if (jwt == null) {
            return buildAnonymousUser();
        }
        User user = userRepository.findByAuth0Id(jwt.getSubject())
            .orElse(null);
        if (user == null) {
            return buildAnonymousUser();
        }
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponseDTO updateCurrentUser(ProfileUpdateDTO request) {
        Jwt jwt = getCurrentJwtOrNull();
        if (jwt == null) {
            return buildAnonymousUser();
        }
        User user = userRepository.findByAuth0Id(jwt.getSubject())
            .orElse(null);
        if (user == null) {
            return buildAnonymousUser();
        }
        userMapper.updateProfile(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponseDTO getById(UUID id) {
        return userRepository.findById(id).map(userMapper::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public UserResponseDTO updateRole(UUID id, Role role) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
        User saved = userRepository.save(user);
        // Added from SOURCE: try/catch for resilience (SPA apps can't use client_credentials)
        try {
            auth0ManagementService.updateUserRole(user.getAuth0Id(), role.name());
        } catch (Exception e) {
            log.warn("Could not sync role to Auth0 via Management API (expected if SPA). Role saved locally.", e);
        }
        return userMapper.toResponse(saved);
    }

    @Override
    public void softDelete(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public void syncFromJwt(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String auth0Nickname = jwt.getClaimAsString("nickname");
        String auth0Name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String picture = jwt.getClaimAsString("picture");

        // TARGET: Enrich from Auth0 Management API when JWT claims are missing
        Auth0ManagementService.Auth0UserProfile profile = null;
        if (!hasText(email) || !hasText(givenName) || !hasText(auth0Name) || !hasText(auth0Nickname) || !hasText(picture)) {
            profile = auth0ManagementService.fetchUserProfile(jwt.getSubject());
            if (profile != null) {
                if (!hasText(email)) {
                    email = profile.getEmail();
                }
                if (!hasText(givenName)) {
                    givenName = profile.getGivenName();
                }
                if (!hasText(familyName)) {
                    familyName = profile.getFamilyName();
                }
                if (!hasText(auth0Name)) {
                    auth0Name = profile.getName();
                }
                if (!hasText(auth0Nickname)) {
                    auth0Nickname = profile.getNickname();
                }
                if (!hasText(picture)) {
                    picture = profile.getPicture();
                }
            }
        }

        String resolvedNickname = chooseBestDisplayName(auth0Nickname, auth0Name, givenName, email);

        User user = userRepository.findByAuth0Id(jwt.getSubject()).orElse(null);
        if (user == null) {
            user = User.builder()
                .auth0Id(jwt.getSubject())
                .email(email)
                .firstName(givenName)
                .lastName(familyName)
                .nickname(resolvedNickname)
                .avatarUrl(picture)
                .role(resolveRole(jwt))
                .authProvider(resolveAuthProvider(jwt))
                .isActive(true)
                .build();
            try {
                userRepository.save(user);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Concurrent user creation detected for auth0Id: {}. Ignoring since another thread succeeded.", jwt.getSubject());
            }
            return;
        }
        boolean updated = false;
        String firstName = givenName;
        String lastName = familyName;

        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            updated = true;
        }
        if (firstName != null && !firstName.equals(user.getFirstName())) {
            user.setFirstName(firstName);
            updated = true;
        }
        if (lastName != null && !lastName.equals(user.getLastName())) {
            user.setLastName(lastName);
            updated = true;
        }
        if (picture != null && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            updated = true;
        }
        if (resolvedNickname != null && !resolvedNickname.equals(user.getNickname())) {
            user.setNickname(resolvedNickname);
            updated = true;
        }

        // Added from SOURCE: Sync role upgrades (Ghost Admin, Coach)
        Role currentExpectedRole = resolveRole(jwt);
        if (currentExpectedRole != user.getRole()) {
            if (currentExpectedRole == Role.ADMIN) {
                user.setRole(Role.ADMIN);
                updated = true;
                log.info("Role synced: Upgraded {} to ADMIN", user.getEmail());
            } else if (currentExpectedRole == Role.COACH && user.getRole() != Role.ADMIN) {
                user.setRole(Role.COACH);
                updated = true;
            }
        }

        if (updated) {
            userRepository.save(user);
        }
    }

    // TARGET: Intelligent display name resolution
    private String chooseBestDisplayName(String auth0Nickname, String auth0Name, String givenName, String email) {
        if (hasText(givenName)) {
            return givenName;
        }
        if (hasText(auth0Name) && !looksLikeMachineIdentifier(auth0Name)) {
            return auth0Name;
        }
        if (hasText(auth0Nickname) && !looksLikeMachineIdentifier(auth0Nickname)) {
            return auth0Nickname;
        }
        if (hasText(email) && email.contains("@")) {
            return email.split("@")[0];
        }
        return auth0Nickname;
    }

    private boolean looksLikeMachineIdentifier(String value) {
        if (!hasText(value)) {
            return false;
        }
        String lower = value.trim().toLowerCase();
        if (lower.startsWith("auth0|")
                || lower.startsWith("google-oauth2|")
                || lower.startsWith("github|")
                || lower.startsWith("facebook|")
                || lower.startsWith("user_")) {
            return true;
        }
        return lower.matches("^[0-9]{8,}$");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // MERGED: resolveRole — TARGET base + SOURCE ghost admin + coach fallback
    private Role resolveRole(Jwt jwt) {
        // SOURCE: Ghost Admin check
        String email = jwt.getClaimAsString("email");
        if (email != null && adminEmail != null && !adminEmail.isBlank()
                && email.equalsIgnoreCase(adminEmail)) {
            log.info("Ghost Admin detected and automatically elevated: {}", email);
            return Role.ADMIN;
        }

        // TARGET: JWT roles claim
        List<String> roles = jwt.getClaimAsStringList("https://codearena.com/roles");
        if (roles != null) {
            if (roles.contains("ADMIN")) {
                return Role.ADMIN;
            }
            if (roles.contains("COACH")) {
                return Role.COACH;
            }
        }

        // SOURCE: Fallback — check coaches table
        String sub = jwt.getSubject();
        if (sub != null && coachRepository.findByUserId(sub).isPresent()) {
            return Role.COACH;
        }

        return Role.PARTICIPANT;
    }

    private AuthProvider resolveAuthProvider(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null) {
            return AuthProvider.LOCAL;
        }
        if (sub.startsWith("google-oauth2|")) {
                return AuthProvider.GOOGLE;
        }
        if (sub.startsWith("github|")) {
                return AuthProvider.GITHUB;
        }
        return AuthProvider.LOCAL;
    }

    // TARGET: Graceful null JWT handling
    private Jwt getCurrentJwtOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        return null;
    }

    // TARGET: Anonymous user fallback
    private UserResponseDTO buildAnonymousUser() {
        return UserResponseDTO.builder()
            .email("guest@local")
            .firstName("Guest")
            .lastName("User")
            .role(Role.PARTICIPANT)
            .authProvider(AuthProvider.LOCAL)
            .isActive(true)
            .build();
    }
}
