package com.codearena.module8_terminalquest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    private String userId; // overridden from JWT — not validated here
    @NotBlank(message = "Answer is required")
    private String answer;
}
