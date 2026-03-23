package com.codearena.user.service;

import com.codearena.user.dto.ProfileUpdateDTO;
import com.codearena.user.dto.UserResponseDTO;
import com.codearena.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface UserService {
    Page<UserResponseDTO> getAll(Pageable pageable);

    UserResponseDTO getCurrentUser();

    UserResponseDTO updateCurrentUser(ProfileUpdateDTO request);

    UserResponseDTO getById(UUID id);

    UserResponseDTO updateRole(UUID id, Role role);

    void softDelete(UUID id);

    void syncFromJwt(Jwt jwt);
}
