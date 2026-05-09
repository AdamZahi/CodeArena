package com.codearena.module8_terminalquest.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAnalysisResponse {
    private Map<String, Double> skillProfile;
    private double overallScore;
    private String predictedWeakness;
    private double weaknessConfidence;
    private Map<String, Object> certificationReadiness;
    private List<Map<String, Object>> recommendations;
    private String playerTitle;
    private String nextTitle;
    private double progressToNextTitle;
}
