package com.codearena.module1_challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitCodeRequest {
    private String code;
    private String language; // ID from Judge0 or standard name e.g. "JAVA" -> "62"
    private Long challengeId;
}
