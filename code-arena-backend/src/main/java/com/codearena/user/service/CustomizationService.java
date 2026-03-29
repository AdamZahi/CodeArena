package com.codearena.user.service;

import com.codearena.user.dto.CustomizationItemDTO;
import com.codearena.user.dto.EquipItemRequest;
import com.codearena.user.dto.UserUnlockDTO;

import java.util.List;

public interface CustomizationService {

    // === ADMIN: Manage items catalog ===
    List<CustomizationItemDTO> getAllItems();
    List<CustomizationItemDTO> getItemsByType(String itemType);
    CustomizationItemDTO createItem(CustomizationItemDTO dto);
    CustomizationItemDTO updateItem(Long id, CustomizationItemDTO dto);
    void deleteItem(Long id);

    // === USER: Profile customization ===
    List<UserUnlockDTO> getMyUnlocks(String keycloakId);
    List<UserUnlockDTO> getMyUnlocksByType(String keycloakId, String itemType);
    void equipItem(String keycloakId, EquipItemRequest request);
    void checkAndGrantUnlocks(String keycloakId);
    void grantDefaultItems(String keycloakId);
}
