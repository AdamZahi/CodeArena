package com.codearena.module4_shop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {

    // Who is buying — participant ID from JWT token
    @NotBlank(message = "Participant ID is required")
    private String participantId;

    // List of items — must not be empty
    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<PurchaseItemRequest> items;
    // ── COUPON CODE (optional) ────────────────────
    private String couponCode;

}