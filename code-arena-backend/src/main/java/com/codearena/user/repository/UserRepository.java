package com.codearena.user.repository;

import com.codearena.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuth0Id(String auth0Id);

    default Optional<User> findByKeycloakId(String keycloakId) {
        return findByAuth0Id(keycloakId);
    }
}
