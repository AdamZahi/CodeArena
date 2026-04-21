package com.codearena.module1_challenge.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintResponseDto {
    private String title;
    private String tags;

    @JsonProperty("predicted_category")
    private String predictedCategory;

    private Double confidence;
    private String hint;
}
