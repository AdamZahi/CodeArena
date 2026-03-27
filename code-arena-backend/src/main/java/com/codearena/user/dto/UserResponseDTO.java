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
    @com.fasterxml.jackson.annotation.JsonProperty("active")
    private boolean active;
    private Instant createdAt;
}
