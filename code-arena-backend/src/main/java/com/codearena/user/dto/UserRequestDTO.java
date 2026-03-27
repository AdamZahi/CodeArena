package com.codearena.user.dto;

import com.codearena.user.entity.AuthProvider;
import com.codearena.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private AuthProvider authProvider;
    private String avatarUrl;
    private String bio;
    @com.fasterxml.jackson.annotation.JsonProperty("active")
    private boolean active;
}
