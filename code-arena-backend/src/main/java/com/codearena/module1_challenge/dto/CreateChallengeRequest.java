package com.codearena.module1_challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChallengeRequest {
    private String title;
    private String description;
    private String difficulty; // EASY, MEDIUM, HARD
    private String tags;
    private List<TestCaseDto> testCases;
}
