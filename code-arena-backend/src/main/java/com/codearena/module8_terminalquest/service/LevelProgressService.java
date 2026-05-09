package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.LevelProgressDto;
import com.codearena.module8_terminalquest.dto.SubmitAnswerRequest;
import com.codearena.module8_terminalquest.dto.SubmitAnswerResponse;

import java.util.List;
import java.util.UUID;

public interface LevelProgressService {
    SubmitAnswerResponse submitAnswer(UUID levelId, SubmitAnswerRequest request);
    SubmitAnswerResponse submitMissionAnswer(UUID missionId, SubmitAnswerRequest request);
    List<LevelProgressDto> getProgressByUser(String userId);
    LevelProgressDto getProgressByUserAndLevel(String userId, UUID levelId);
    LevelProgressDto getProgressByUserAndMission(String userId, UUID missionId);
}
