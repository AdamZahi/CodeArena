package com.codearena.module2_battle.repository;

import com.codearena.module2_battle.entity.SharedResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SharedResultRepository extends JpaRepository<SharedResult, String> {

    Optional<SharedResult> findByBattleRoomIdAndRequestedByUserId(String battleRoomId, String requestedByUserId);

    @Modifying
    @Query("DELETE FROM SharedResult s WHERE s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
