package com.codearena.user.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipItemRequest {
    private String itemType; // ICON, BORDER, TITLE
    private String itemKey;  // The key of the item to equip
}
