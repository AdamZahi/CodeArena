package com.codearena.module7_coaching.repository;

import com.codearena.module7_coaching.entity.SessionReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionReservationRepository extends JpaRepository<SessionReservation, UUID> {
    List<SessionReservation> findByUserId(String userId);

    List<SessionReservation> findByUserIdAndCancelledFalse(String userId);

    List<SessionReservation> findBySessionId(UUID sessionId);

    Optional<SessionReservation> findBySessionIdAndUserIdAndCancelledFalse(UUID sessionId, String userId);

    boolean existsBySessionIdAndUserIdAndCancelledFalse(UUID sessionId, String userId);

    void deleteBySessionId(UUID sessionId);
}
