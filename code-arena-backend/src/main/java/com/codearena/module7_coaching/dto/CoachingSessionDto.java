package com.codearena.module7_coaching.dto;

import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.SessionStatus;
import com.codearena.module7_coaching.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingSessionDto {
    private UUID id;
    private String coachId;
    private String learnerId;

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "Le langage est obligatoire")
    private ProgrammingLanguage language;

    @NotNull(message = "Le niveau est obligatoire")
    private SkillLevel level;

    @NotNull(message = "La date prévue est obligatoire")
    @Future(message = "La date de la session doit être dans le futur")
    private LocalDateTime scheduledAt;

    @NotNull(message = "La durée est obligatoire")
    @Min(value = 15, message = "La durée minimale est de 15 minutes")
    private Integer durationMinutes;

    private SessionStatus status;
    private String meetingUrl;

    @Min(value = 1, message = "Le nombre maximum de participants doit être d'au moins 1")
    private Integer maxParticipants;

    private Integer currentParticipants;
}
