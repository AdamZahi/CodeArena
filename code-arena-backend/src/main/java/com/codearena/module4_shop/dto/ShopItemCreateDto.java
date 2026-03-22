package com.codearena.module4_shop.dto;

import com.codearena.module4_shop.enums.ItemType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopItemCreateDto {

    // Matches Angular validation: required, min 3 chars
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    // Matches Angular validation: required, min 10 chars
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    // Matches Angular validation: price > 0, max 9999
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "9999.99", message = "Price cannot exceed 9999.99")
    private Double price;

    // Matches Angular validation: stock >= 0
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Max(value = 99999, message = "Stock cannot exceed 99999")
    private Integer stock;

    // Optional but must be valid URL if provided
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "Image URL must start with http:// or https://"
    )
    private String imageUrl;

    // Matches Angular validation: category required
    @NotNull(message = "Category is required")
    private ItemType category;
}