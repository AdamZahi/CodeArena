package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.ArenNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface NotificationRepository extends JpaRepository<ArenNotification, Long> {

    List<ArenNotification> findByUserKeycloakIdOrderByCreatedAtDesc(String keycloakId);

    @Modifying
    @Transactional
    @Query("UPDATE ArenNotification n SET n.read = true WHERE n.user.keycloakId = :keycloakId")
    void markAllAsRead(@Param("keycloakId") String keycloakId);
}