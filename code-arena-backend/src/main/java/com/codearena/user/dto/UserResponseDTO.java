package com.codearena.user.dto;

import com.codearena.user.entity.AuthProvider;
import com.codearena.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private UUID id;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private AuthProvider authProvider;
    private String avatarUrl;
    private String bio;
    private boolean isActive;
    
    // Customization & Stats
    private Integer level;
    private Long totalXp;
    private String nickname;
    private String rankTier;
    private String rankDivision;
    private Integer leaguePoints;
    private Integer matchesPlayed;
    private Integer matchesWon;
    private Integer honorLevel;
    private String activeIconId;
    private String activeBorderId;
    private String activeTitle;
    private String activeBadge1;
    private String activeBadge2;
    private String activeBadge3;

    private Instant createdAt;
}
