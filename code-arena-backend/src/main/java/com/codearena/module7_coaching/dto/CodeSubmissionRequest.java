package com.codearena.module7_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmissionRequest {
    @NotBlank(message = "L'ID de l'étudiant est obligatoire")
    private String studentId;
    
    @NotBlank(message = "Le langage est obligatoire")
    private String language;
    
    @NotBlank(message = "Le code est obligatoire")
    private String code;
}
