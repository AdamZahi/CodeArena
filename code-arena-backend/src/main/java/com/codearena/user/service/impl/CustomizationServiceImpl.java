package com.codearena.user.service.impl;

import com.codearena.user.dto.CustomizationItemDTO;
import com.codearena.user.dto.EquipItemRequest;
import com.codearena.user.dto.UserUnlockDTO;
import com.codearena.user.entity.CustomizationItem;
import com.codearena.user.entity.User;
import com.codearena.user.entity.UserUnlock;
import com.codearena.user.repository.CustomizationItemRepository;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.repository.UserUnlockRepository;
import com.codearena.user.service.CustomizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomizationServiceImpl implements CustomizationService {

    private final CustomizationItemRepository itemRepo;
    private final UserUnlockRepository unlockRepo;
    private final UserRepository userRepo;

    // ========== ADMIN: Item Catalog CRUD ==========

    @Override
    public List<CustomizationItemDTO> getAllItems() {
        return itemRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<CustomizationItemDTO> getItemsByType(String itemType) {
        return itemRepo.findByItemType(itemType.toUpperCase()).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomizationItemDTO createItem(CustomizationItemDTO dto) {
        CustomizationItem item = CustomizationItem.builder()
                .itemType(dto.getItemType().toUpperCase())
                .itemKey(dto.getItemKey())
                .label(dto.getLabel())
                .imageUrl(dto.getImageUrl())
                .rarity(dto.getRarity() != null ? dto.getRarity().toUpperCase() : "COMMON")
                .description(dto.getDescription())
                .unlockType(dto.getUnlockType() != null ? dto.getUnlockType().toUpperCase() : "DEFAULT")
                .unlockThreshold(dto.getUnlockThreshold() != null ? dto.getUnlockThreshold() : 0)
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .build();
        return toDTO(itemRepo.save(item));
    }

    @Override
    @Transactional
    public CustomizationItemDTO updateItem(Long id, CustomizationItemDTO dto) {
        CustomizationItem item = itemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));
        if (dto.getItemType() != null) item.setItemType(dto.getItemType().toUpperCase());
        if (dto.getItemKey() != null) item.setItemKey(dto.getItemKey());
        if (dto.getLabel() != null) item.setLabel(dto.getLabel());
        if (dto.getImageUrl() != null) item.setImageUrl(dto.getImageUrl());
        if (dto.getRarity() != null) item.setRarity(dto.getRarity().toUpperCase());
        if (dto.getDescription() != null) item.setDescription(dto.getDescription());
        if (dto.getUnlockType() != null) item.setUnlockType(dto.getUnlockType().toUpperCase());
        if (dto.getUnlockThreshold() != null) item.setUnlockThreshold(dto.getUnlockThreshold());
        if (dto.getIsDefault() != null) item.setIsDefault(dto.getIsDefault());
        return toDTO(itemRepo.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        itemRepo.deleteById(id);
    }

    // ========== USER: Unlocks & Equip ==========

    @Override
    public List<UserUnlockDTO> getMyUnlocks(String keycloakId) {
        List<UserUnlock> unlocks = unlockRepo.findByUserId(keycloakId);
        return unlocks.stream().map(this::toUnlockDTO).collect(Collectors.toList());
    }

    @Override
    public List<UserUnlockDTO> getMyUnlocksByType(String keycloakId, String itemType) {
        List<UserUnlock> unlocks = unlockRepo.findByUserIdAndItemType(keycloakId, itemType.toUpperCase());
        return unlocks.stream().map(this::toUnlockDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void equipItem(String keycloakId, EquipItemRequest request) {
        // Verify the user owns this item
        boolean owns = unlockRepo.existsByUserIdAndItemKey(keycloakId, request.getItemKey());
        if (!owns) {
            throw new RuntimeException("You haven't unlocked this item: " + request.getItemKey());
        }

        User user = userRepo.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        switch (request.getItemType().toUpperCase()) {
            case "ICON":
                user.setActiveIconId(request.getItemKey());
                break;
            case "BORDER":
                user.setActiveBorderId(request.getItemKey());
                break;
            case "TITLE":
                user.setActiveTitle(request.getItemKey());
                break;
            case "BADGE":
            case "BADGES":
                equipBadge(user, request.getItemKey());
                break;
            default:
                throw new RuntimeException("Invalid item type: " + request.getItemType());
        }
        userRepo.save(user);
    }

    private void equipBadge(User user, String badgeKey) {
        // If already equipped in ANY slot, do nothing (to avoid duplicates)
        if (badgeKey.equals(user.getActiveBadge1()) || 
            badgeKey.equals(user.getActiveBadge2()) || 
            badgeKey.equals(user.getActiveBadge3())) {
            return;
        }

        // Cycle through slots
        if (user.getActiveBadge1() == null) {
            user.setActiveBadge1(badgeKey);
        } else if (user.getActiveBadge2() == null) {
            user.setActiveBadge2(badgeKey);
        } else if (user.getActiveBadge3() == null) {
            user.setActiveBadge3(badgeKey);
        } else {
            // All slots full? Replace the oldest (Cycle logic)
            user.setActiveBadge1(badgeKey);
        }
    }

    @Override
    @Transactional
    public void grantDefaultItems(String keycloakId) {
        List<CustomizationItem> defaults = itemRepo.findByIsDefaultTrue();
        for (CustomizationItem item : defaults) {
            if (!unlockRepo.existsByUserIdAndItemKey(keycloakId, item.getItemKey())) {
                UserUnlock unlock = UserUnlock.builder()
                        .userId(keycloakId)
                        .itemType(item.getItemType())
                        .itemKey(item.getItemKey())
                        .acquisitionSource("DEFAULT")
                        .build();
                unlockRepo.save(unlock);
            }
        }
    }

    @Override
    @Transactional
    public void checkAndGrantUnlocks(String keycloakId) {
        User user = userRepo.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Grant items by LEVEL threshold
        List<CustomizationItem> levelItems = itemRepo
                .findByUnlockTypeAndUnlockThresholdLessThanEqual("LEVEL", user.getLevel());
        grantItems(keycloakId, levelItems, "LEVEL_UP");

        // Grant items by XP threshold
        List<CustomizationItem> xpItems = itemRepo
                .findByUnlockTypeAndUnlockThresholdLessThanEqual("XP", user.getTotalXp().intValue());
        grantItems(keycloakId, xpItems, "XP_MILESTONE");

        log.info("Checked and granted unlocks for user {}", keycloakId);
    }

    private void grantItems(String keycloakId, List<CustomizationItem> items, String source) {
        for (CustomizationItem item : items) {
            if (!unlockRepo.existsByUserIdAndItemKey(keycloakId, item.getItemKey())) {
                UserUnlock unlock = UserUnlock.builder()
                        .userId(keycloakId)
                        .itemType(item.getItemType())
                        .itemKey(item.getItemKey())
                        .acquisitionSource(source)
                        .build();
                unlockRepo.save(unlock);
                log.info("Unlocked {} for user {} (source: {})", item.getItemKey(), keycloakId, source);
            }
        }
    }

    // ========== Mappers ==========

    private CustomizationItemDTO toDTO(CustomizationItem e) {
        return CustomizationItemDTO.builder()
                .id(e.getId())
                .itemType(e.getItemType())
                .itemKey(e.getItemKey())
                .label(e.getLabel())
                .imageUrl(e.getImageUrl())
                .rarity(e.getRarity())
                .description(e.getDescription())
                .unlockType(e.getUnlockType())
                .unlockThreshold(e.getUnlockThreshold())
                .isDefault(e.getIsDefault())
                .build();
    }

    private UserUnlockDTO toUnlockDTO(UserUnlock u) {
        UserUnlockDTO dto = UserUnlockDTO.builder()
                .id(u.getId())
                .userId(u.getUserId())
                .itemType(u.getItemType())
                .itemKey(u.getItemKey())
                .unlockedAt(u.getUnlockedAt())
                .acquisitionSource(u.getAcquisitionSource())
                .build();

        // Enrich with item metadata
        itemRepo.findByItemKey(u.getItemKey()).ifPresent(item -> {
            dto.setLabel(item.getLabel());
            dto.setImageUrl(item.getImageUrl());
            dto.setRarity(item.getRarity());
        });

        return dto;
    }
}
