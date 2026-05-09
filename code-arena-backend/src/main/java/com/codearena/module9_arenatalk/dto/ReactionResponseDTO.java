package com.codearena.module9_arenatalk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class ReactionResponseDTO {
    private Long messageId;
    private Map<String, Long> counts;      // emoji -> count
    private Map<String, Boolean> reacted;  // emoji -> did current user react
}