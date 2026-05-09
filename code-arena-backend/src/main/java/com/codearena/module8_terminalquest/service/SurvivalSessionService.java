package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.StartSurvivalRequest;
import com.codearena.module8_terminalquest.dto.SurvivalAnswerRequest;
import com.codearena.module8_terminalquest.dto.SurvivalAnswerResponse;
import com.codearena.module8_terminalquest.dto.SurvivalSessionDto;

import java.util.List;
import java.util.UUID;

public interface SurvivalSessionService {
    SurvivalSessionDto startSession(StartSurvivalRequest request);
    SurvivalAnswerResponse submitAnswer(UUID sessionId, SurvivalAnswerRequest request);
    SurvivalSessionDto endSession(UUID sessionId);
    List<SurvivalSessionDto> getSessionsByUser(String userId);
    SurvivalSessionDto getSessionById(UUID id);
}
