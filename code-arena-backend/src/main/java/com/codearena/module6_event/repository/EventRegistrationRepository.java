package com.codearena.module6_event.repository;

import com.codearena.module6_event.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    // TODO: Add custom query methods.
}
