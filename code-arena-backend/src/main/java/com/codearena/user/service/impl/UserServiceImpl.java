package com.codearena.user.service.impl;

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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Override
    public Page<UserResponseDTO> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    public UserResponseDTO getCurrentUser() {
        Jwt jwt = getCurrentJwtOrNull();
        if (jwt == null) {
            return buildAnonymousUser();
        }
        try {
            User user = userRepository.findByAuth0Id(jwt.getSubject())
                .orElse(null);
            if (user == null) {
                return buildAnonymousUser();
            }
            return userMapper.toResponse(user);
        } catch (DataAccessException ex) {
            log.debug("Falling back to anonymous user because the database is unavailable: {}", ex.getMostSpecificCause().getMessage());
            return buildAnonymousUser();
        }
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

        String requestedEmail = normalize(request.getEmail());
        if (requestedEmail != null && (user.getEmail() == null || user.getEmail().isBlank())) {
            user.setEmail(requestedEmail);
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
        auth0ManagementService.updateUserRole(user.getAuth0Id(), role.name());
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
        try {
            User user = userRepository.findByAuth0Id(jwt.getSubject()).orElse(null);
            String email = resolveEmail(jwt);
            String firstName = resolveFirstName(jwt);
            String lastName = resolveLastName(jwt, firstName);
            String nickname = resolveNickname(jwt);
            String fullName = normalize(jwt.getClaimAsString("name"));
            String picture = normalize(jwt.getClaimAsString("picture"));

            if (email == null || firstName == null || lastName == null || nickname == null || picture == null) {
                Auth0ManagementService.Auth0UserProfile profile = auth0ManagementService.fetchUserProfile(jwt.getSubject());
                if (profile != null) {
                    email = firstNonBlank(email, normalize(profile.getEmail()));
                    nickname = firstNonBlank(nickname, normalize(profile.getNickname()));
                    fullName = firstNonBlank(fullName, normalize(profile.getName()));
                    picture = firstNonBlank(picture, normalize(profile.getPicture()));

                    String profileFirstName = firstNonBlank(normalize(profile.getGivenName()), extractFirstName(fullName));
                    firstName = firstNonBlank(firstName, profileFirstName);
                    lastName = firstNonBlank(
                        lastName,
                        normalize(profile.getFamilyName()),
                        extractLastName(fullName, firstName)
                    );
                }
            }

            String displayName = chooseBestDisplayName(nickname, fullName, firstName, email);

            if (user == null) {
                user = User.builder()
                    .auth0Id(jwt.getSubject())
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .nickname(displayName)
                    .avatarUrl(picture)
                    .role(resolveRole(jwt))
                    .authProvider(resolveAuthProvider(jwt))
                    .isActive(true)
                    .build();
                try {
                    userRepository.save(user);
                } catch (DataIntegrityViolationException e) {
                    log.warn("Concurrent user creation detected for auth0Id: {}. Ignoring since another thread succeeded.", jwt.getSubject());
                }
                return;
            }

            boolean updated = false;

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
            if (displayName != null && !displayName.equals(user.getNickname())) {
                user.setNickname(displayName);
                updated = true;
            }
            if (picture != null && !picture.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(picture);
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
            }
        } catch (DataAccessException ex) {
            log.debug("Skipping JWT user sync for {} because the database is unavailable: {}", jwt.getSubject(), ex.getMostSpecificCause().getMessage());
        }
    }

    private String resolveEmail(Jwt jwt) {
        return firstNonBlank(
            normalize(jwt.getClaimAsString("email")),
            normalize(jwt.getClaimAsString("https://codearena.com/email")),
            normalize(jwt.getClaimAsString("upn")),
            normalize(jwt.getClaimAsString("preferred_username"))
        );
    }

    private String resolveFirstName(Jwt jwt) {
        String givenName = firstNonBlank(
            normalize(jwt.getClaimAsString("given_name")),
            normalize(jwt.getClaimAsString("https://codearena.com/given_name"))
        );
        if (givenName != null) {
            return givenName;
        }

        String fullName = normalize(jwt.getClaimAsString("name"));
        if (fullName == null) {
            fullName = normalize(jwt.getClaimAsString("https://codearena.com/name"));
        }
        if (fullName != null) {
            String[] parts = fullName.split("\\s+");
            return parts.length > 0 ? normalize(parts[0]) : null;
        }

        return normalize(jwt.getClaimAsString("nickname"));
    }

    private String resolveLastName(Jwt jwt, String resolvedFirstName) {
        String familyName = firstNonBlank(
            normalize(jwt.getClaimAsString("family_name")),
            normalize(jwt.getClaimAsString("https://codearena.com/family_name"))
        );
        if (familyName != null) {
            return familyName;
        }

        String fullName = normalize(jwt.getClaimAsString("name"));
        if (fullName == null) {
            fullName = normalize(jwt.getClaimAsString("https://codearena.com/name"));
        }
        if (fullName == null || resolvedFirstName == null) {
            return null;
        }

        return extractLastName(fullName, resolvedFirstName);
    }

    private String resolveNickname(Jwt jwt) {
        return firstNonBlank(
            normalize(jwt.getClaimAsString("nickname")),
            normalize(jwt.getClaimAsString("preferred_username"))
        );
    }

    private String extractFirstName(String fullName) {
        String normalized = normalize(fullName);
        if (normalized == null) {
            return null;
        }
        String[] parts = normalized.split("\\s+");
        return parts.length > 0 ? normalize(parts[0]) : null;
    }

    private String extractLastName(String fullName, String resolvedFirstName) {
        String normalized = normalize(fullName);
        if (normalized == null || resolvedFirstName == null) {
            return null;
        }

        String[] parts = normalized.split("\\s+");
        if (parts.length <= 1) {
            return null;
        }
        return normalize(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String chooseBestDisplayName(String nickname, String fullName, String givenName, String email) {
        if (hasText(nickname) && !looksLikeMachineIdentifier(nickname)) {
            return nickname;
        }
        if (hasText(givenName)) {
            return givenName;
        }
        if (hasText(fullName) && !looksLikeMachineIdentifier(fullName)) {
            return fullName;
        }
        if (hasText(email) && email.contains("@")) {
            return email.split("@")[0];
        }
        return nickname;
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

    private Role resolveRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("https://codearena.com/roles");
        if (roles != null) {
            if (roles.contains("ADMIN")) {
                return Role.ADMIN;
            }
            if (roles.contains("COACH")) {
                return Role.COACH;
            }
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

    private Jwt getCurrentJwtOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        return null;
    }

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
