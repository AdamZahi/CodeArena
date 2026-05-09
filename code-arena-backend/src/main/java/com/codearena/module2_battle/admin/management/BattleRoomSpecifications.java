package com.codearena.module2_battle.admin.management;

import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class BattleRoomSpecifications {

    private BattleRoomSpecifications() {}

    public static Specification<BattleRoom> hasStatus(BattleRoomStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<BattleRoom> hasHost(String hostId) {
        return (hostId == null || hostId.isBlank()) ? null
                : (root, q, cb) -> cb.equal(root.get("hostId"), hostId);
    }

    public static Specification<BattleRoom> createdBetween(Instant from, Instant to) {
        if (from == null && to == null) return null;
        return (root, q, cb) -> {
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}
