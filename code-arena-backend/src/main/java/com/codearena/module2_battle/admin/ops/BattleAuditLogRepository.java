package com.codearena.module2_battle.admin.ops;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface BattleAuditLogRepository extends JpaRepository<BattleAuditLog, UUID> {

    Page<BattleAuditLog> findByAdminIdOrderByPerformedAtDesc(String adminId, Pageable pageable);

    Page<BattleAuditLog> findByActionOrderByPerformedAtDesc(String action, Pageable pageable);

    Page<BattleAuditLog> findByTargetRoomIdOrderByPerformedAtDesc(String targetRoomId, Pageable pageable);

    @Query("SELECT a FROM BattleAuditLog a ORDER BY a.performedAt DESC")
    Page<BattleAuditLog> findAllOrderByPerformedAtDesc(Pageable pageable);
}
