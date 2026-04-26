package com.codearena.module2_battle.admin.ops;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "battle_audit_log", indexes = {
        @Index(name = "idx_audit_admin_id", columnList = "admin_id"),
        @Index(name = "idx_audit_performed_at", columnList = "performed_at"),
        @Index(name = "idx_audit_target_room", columnList = "target_room_id"),
        @Index(name = "idx_audit_action", columnList = "action")
})
public class BattleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private String adminId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_room_id")
    private String targetRoomId;

    /** JSON blob with before/after state or other context. */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
