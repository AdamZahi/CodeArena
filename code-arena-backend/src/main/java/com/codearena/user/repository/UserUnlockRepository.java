package com.codearena.user.repository;

import com.codearena.user.entity.UserUnlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserUnlockRepository extends JpaRepository<UserUnlock, Long> {
    List<UserUnlock> findByUserId(String userId);
    List<UserUnlock> findByUserIdAndItemType(String userId, String itemType);
    Optional<UserUnlock> findByUserIdAndItemKey(String userId, String itemKey);
    boolean existsByUserIdAndItemKey(String userId, String itemKey);
}
