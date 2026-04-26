package com.codearena.module7_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionFeedbackDto {
    @NotBlank(message = "L'ID du coach est obligatoire")
    private String coachId;

    private String userId;

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    private Double rating;

    @NotBlank(message = "Le commentaire est obligatoire")
    private String comment;
    private String createdAt;
}
