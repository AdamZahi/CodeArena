package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.ArenNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface NotificationRepository extends JpaRepository<ArenNotification, Long> {

    List<ArenNotification> findByUserAuth0IdOrderByCreatedAtDesc(String auth0Id);

    @Modifying
    @Transactional
    @Query("UPDATE ArenNotification n SET n.read = true WHERE n.user.auth0Id = :auth0Id")
    void markAllAsRead(@Param("auth0Id") String auth0Id);
}