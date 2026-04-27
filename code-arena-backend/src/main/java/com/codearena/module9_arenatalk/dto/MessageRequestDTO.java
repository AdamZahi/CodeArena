package com.codearena.module9_arenatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageRequestDTO {

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;
}