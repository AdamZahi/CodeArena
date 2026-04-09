package com.codearena.user.repository;

import com.codearena.user.entity.CustomizationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomizationItemRepository extends JpaRepository<CustomizationItem, Long> {
    List<CustomizationItem> findByItemType(String itemType);
    List<CustomizationItem> findByIsDefaultTrue();
    Optional<CustomizationItem> findByItemKey(String itemKey);
    List<CustomizationItem> findByUnlockTypeAndUnlockThresholdLessThanEqual(String unlockType, Integer threshold);
}
