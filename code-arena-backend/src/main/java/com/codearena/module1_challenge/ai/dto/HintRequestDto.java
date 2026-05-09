package com.codearena.module1_challenge.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintRequestDto {
    private String title;
    private String tags;
    private String description;
    private String difficulty;
}
