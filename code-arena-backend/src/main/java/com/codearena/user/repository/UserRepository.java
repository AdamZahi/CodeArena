package com.codearena.user.repository;

import com.codearena.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuth0Id(String auth0Id);

    default Optional<User> findByKeycloakId(String keycloakId) {
        return findByAuth0Id(keycloakId);
    }

    Page<User> findAllByOrderByTotalXpDesc(Pageable pageable);

    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.totalXp > :xp")
    int countUsersByTotalXpGreaterThan(@Param("xp") long xp);
}
