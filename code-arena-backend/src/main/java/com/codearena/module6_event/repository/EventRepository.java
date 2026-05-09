package com.codearena.module6_event.repository;

import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<ProgrammingEvent, UUID> {

    List<ProgrammingEvent> findByType(EventType type);

    List<ProgrammingEvent> findByStatus(String status);

    List<ProgrammingEvent> findByTypeAndStatus(EventType type, String status);

    List<ProgrammingEvent> findByStartDateBetween(LocalDateTime start, LocalDateTime end);
}
