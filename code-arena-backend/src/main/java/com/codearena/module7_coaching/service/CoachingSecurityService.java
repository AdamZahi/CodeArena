package com.codearena.module7_coaching.service;

import com.codearena.user.entity.Role;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("coachingSecurity")
@RequiredArgsConstructor
public class CoachingSecurityService {

    private final UserRepository userRepository;

    public boolean isAdmin(Jwt jwt) {
        if (jwt == null) return false;

        // 1. Check JWT Claims
        List<String> roles = jwt.getClaimAsStringList("https://codearena.com/roles");
        if (roles != null && roles.contains("ADMIN")) {
            return true;
        }

        // 2. Fallback to Database
        return userRepository.findByAuth0Id(jwt.getSubject())
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }

    public boolean isCoachOrAdmin(Jwt jwt) {
        if (jwt == null) return false;

        // 1. Check JWT Claims
        List<String> roles = jwt.getClaimAsStringList("https://codearena.com/roles");
        if (roles != null && (roles.contains("ADMIN") || roles.contains("COACH"))) {
            return true;
        }

        // 2. Fallback to Database
        return userRepository.findByAuth0Id(jwt.getSubject())
                .map(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.COACH)
                .orElse(false);
    }
}
