package com.codearena.module9_arenatalk.dto;

import com.codearena.module9_arenatalk.entity.HubCategory;
import com.codearena.module9_arenatalk.entity.HubVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHubRequestDTO {

    @NotBlank(message = "Community name is required")
    @Size(min = 3, max = 30, message = "Community name must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9 _-]+$",
            message = "Community name can only contain letters, numbers, spaces, underscores and hyphens"
    )
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 200, message = "Description must be between 10 and 200 characters")
    private String description;

    @Pattern(
            regexp = "^(https?:\\/\\/.*)?$",
            message = "Banner URL must be a valid URL"
    )
    private String bannerUrl;

    @Pattern(
            regexp = "^(https?:\\/\\/.*)?$",
            message = "Icon URL must be a valid URL"
    )
    private String iconUrl;

    @NotNull(message = "Category is required")
    private HubCategory category;

    @NotNull(message = "Visibility is required")
    private HubVisibility visibility;

    // ← ajouté
    private String keycloakId;
}