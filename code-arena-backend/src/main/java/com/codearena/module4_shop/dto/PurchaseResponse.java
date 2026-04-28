package com.codearena.module4_shop.dto;

import com.codearena.module4_shop.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {

    private UUID id;
    private String participantId;
    private String participantName;
    private Double totalPrice;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<PurchaseItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseItemResponse {
        private UUID id;
        private ShopItemDto product;
        private Integer quantity;
        private Double unitPrice;
    }
}