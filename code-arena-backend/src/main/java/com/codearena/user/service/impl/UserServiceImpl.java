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
        String email = jwt.getClaimAsString("email");
        String auth0Nickname = jwt.getClaimAsString("nickname");
        String auth0Name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String resolvedNickname = chooseBestDisplayName(auth0Nickname, auth0Name, givenName, email);

        User user = userRepository.findByAuth0Id(jwt.getSubject()).orElse(null);
        if (user == null) {
            user = User.builder()
                .auth0Id(jwt.getSubject())
                .email(email)
                .firstName(givenName)
                .lastName(jwt.getClaimAsString("family_name"))
                .nickname(resolvedNickname)
                .avatarUrl(jwt.getClaimAsString("picture"))
                .role(resolveRole(jwt))
                .authProvider(resolveAuthProvider(jwt))
                .isActive(true)
                .build();
            userRepository.save(user);
            return;
        }
        boolean updated = false;
        String firstName = givenName;
        String lastName = jwt.getClaimAsString("family_name");
        String picture = jwt.getClaimAsString("picture");

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

        if (updated) {
            userRepository.save(user);
        }
    }

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
