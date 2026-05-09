package com.codearena.module8_terminalquest.repository;

import com.codearena.module8_terminalquest.entity.ActivityLog;
import com.codearena.module8_terminalquest.entity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ActivityLog> findByUserIdAndActivityTypeOrderByCreatedAtDesc(String userId, ActivityType activityType);

    @Query("SELECT a.activityType, COUNT(a) FROM ActivityLog a WHERE a.userId = :userId GROUP BY a.activityType")
    List<Object[]> countByActivityTypeForUser(@Param("userId") String userId);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.userId = :userId AND a.createdAt >= :since")
    long countRecentActivity(@Param("userId") String userId, @Param("since") Instant since);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.createdAt >= :since AND a.activityType = :type")
    long countByTypeAndSince(@Param("type") ActivityType type, @Param("since") Instant since);
}
