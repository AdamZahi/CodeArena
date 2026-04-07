package com.codearena.module7_coaching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionFeedbackDto {
    private String coachId;
    private String userId;
    private Double rating;
    private String comment;
    private String createdAt;
}
