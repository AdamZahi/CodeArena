package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDto {
    private UUID attemptId;
    private UUID quizId;
    private Integer score;
    private Integer totalPoints;
    private Double percentage;
    private SkillLevel level;
    private List<String> weakTopics;
    private List<AnswerResultDto> answerResults;
    private Instant completedAt;
}
