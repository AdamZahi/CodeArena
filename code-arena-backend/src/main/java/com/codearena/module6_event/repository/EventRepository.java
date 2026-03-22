package com.codearena.module6_event.repository;

import com.codearena.module6_event.entity.ProgrammingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository extends JpaRepository<ProgrammingEvent, UUID> {
    // TODO: Add custom query methods.
}
