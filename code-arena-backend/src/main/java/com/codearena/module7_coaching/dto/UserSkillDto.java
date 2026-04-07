package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillDto {
    private String userId;
    private ProgrammingLanguage language;
    private SkillLevel level;
    private Double scoreAverage;
}
