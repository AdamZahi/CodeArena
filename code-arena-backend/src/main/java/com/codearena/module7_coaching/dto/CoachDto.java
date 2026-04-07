package com.codearena.module7_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachDto {
    private UUID id;
    private String userId;
    private String name;
    private String bio;
    private List<String> specializations;
    private Double rating;
    private Integer totalSessions;
}
