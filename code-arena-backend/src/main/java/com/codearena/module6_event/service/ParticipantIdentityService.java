package com.codearena.module6_event.service;

import com.codearena.module2_battle.util.UserDisplayUtils;
import com.codearena.user.entity.AuthProvider;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.service.impl.Auth0ManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantIdentityService {

    private static final String UNKNOWN_HACKER = "Unknown Hacker";

    private final UserRepository userRepository;
    private final Auth0ManagementService auth0ManagementService;

    @Transactional
    public String resolveDisplayName(String participantId) {
        if (participantId == null || participantId.isBlank()) {
            return UNKNOWN_HACKER;
        }

        return userRepository.findByAuth0Id(participantId)
                .map(user -> {
                    String displayName = UserDisplayUtils.resolveDisplayName(user);
                    if (UNKNOWN_HACKER.equals(displayName)
                            || looksLikeMachineIdentifier(displayName)
                            || hasMissingIdentityData(user)) {
                        return hydrateAndResolve(participantId);
                    }
                    return displayName;
                })
                .orElseGet(() -> hydrateAndResolve(participantId));
    }

    @Transactional
    public String resolveEmail(String participantId) {
        if (participantId == null || participantId.isBlank()) {
            return null;
        }

        return userRepository.findByAuth0Id(participantId)
                .map(user -> {
                    if (hasText(user.getEmail())) {
                        return user.getEmail();
                    }
                    hydrateAndResolve(participantId);
                    return userRepository.findByAuth0Id(participantId)
                            .map(User::getEmail)
                            .orElse(null);
                })
                .orElseGet(() -> {
                    hydrateAndResolve(participantId);
                    return userRepository.findByAuth0Id(participantId)
                            .map(User::getEmail)
                            .orElse(null);
                });
    }

    private String hydrateAndResolve(String participantId) {
        Auth0ManagementService.Auth0UserProfile profile = auth0ManagementService.fetchUserProfile(participantId);
        if (profile == null) {
            return fallbackFromParticipantId(participantId);
        }

        User user = userRepository.findByAuth0Id(participantId).orElseGet(() -> User.builder()
                .auth0Id(participantId)
                .role(Role.PARTICIPANT)
                .authProvider(resolveAuthProvider(participantId))
                .isActive(true)
                .build());

        if (hasText(profile.getEmail())) {
            user.setEmail(profile.getEmail());
        }
        if (hasText(profile.getGivenName())) {
            user.setFirstName(profile.getGivenName());
        } else if (hasText(profile.getName()) && !hasText(user.getFirstName())) {
            user.setFirstName(profile.getName());
        }
        if (hasText(profile.getFamilyName())) {
            user.setLastName(profile.getFamilyName());
        }
        String preferredDisplayName = chooseBestDisplayName(
                profile.getNickname(), profile.getName(), profile.getGivenName(), profile.getEmail());
        if (hasText(preferredDisplayName)) {
            user.setNickname(preferredDisplayName);
        }
        if (hasText(profile.getPicture())) {
            user.setAvatarUrl(profile.getPicture());
        }
        if (user.getRole() == null) {
            user.setRole(Role.PARTICIPANT);
        }
        if (user.getAuthProvider() == null) {
            user.setAuthProvider(resolveAuthProvider(participantId));
        }
        user.setActive(true);

        User saved = userRepository.save(user);
        log.debug("Hydrated user profile for {}", participantId);
        return UserDisplayUtils.resolveDisplayName(saved);
    }

    private String fallbackFromParticipantId(String participantId) {
        if (participantId == null || participantId.isBlank()) {
            return UNKNOWN_HACKER;
        }
        String value = participantId.contains("|") ? participantId.split("\\|", 2)[1] : participantId;
        if (value.length() > 20) {
            return "user_" + value.substring(value.length() - 8);
        }
        return value;
    }

    private AuthProvider resolveAuthProvider(String auth0Id) {
        if (auth0Id == null) {
            return AuthProvider.LOCAL;
        }
        if (auth0Id.startsWith("google-oauth2|")) {
            return AuthProvider.GOOGLE;
        }
        if (auth0Id.startsWith("github|")) {
            return AuthProvider.GITHUB;
        }
        return AuthProvider.LOCAL;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String chooseBestDisplayName(String nickname, String name, String givenName, String email) {
        if (hasText(givenName)) {
            return givenName;
        }
        if (hasText(name) && !looksLikeMachineIdentifier(name)) {
            return name;
        }
        if (hasText(nickname) && !looksLikeMachineIdentifier(nickname)) {
            return nickname;
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

    private boolean hasMissingIdentityData(User user) {
        return !hasText(user.getNickname())
                && !hasText(user.getFirstName())
                && !hasText(user.getEmail());
    }
}
