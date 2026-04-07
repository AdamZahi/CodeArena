package com.codearena.user.repository;

import com.codearena.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import com.codearena.user.entity.Role;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKeycloakId(String keycloakId);
    List<User> findByRole(Role role);
}
