package com.codearena.module0_user.repository;

import com.codearena.module0_user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByUsername(String username);
}
