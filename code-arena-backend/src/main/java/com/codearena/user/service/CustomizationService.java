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
    List<UserUnlockDTO> getMyUnlocks(String auth0Id);
    List<UserUnlockDTO> getMyUnlocksByType(String auth0Id, String itemType);
    void equipItem(String auth0Id, EquipItemRequest request);
    void checkAndGrantUnlocks(String auth0Id);
    void grantDefaultItems(String auth0Id);
}
