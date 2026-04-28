package com.codearena.module7_coaching.service;

import com.codearena.module7_coaching.dto.*;

import java.util.List;
import java.util.UUID;

public interface QuizService {
    List<QuizDto> getAllQuizzes();
    QuizDto getQuizById(UUID quizId);
    QuizResultDto submitQuiz(String userId, SubmitQuizRequest request);
    List<QuizResultDto> getUserHistory(String userId);
    QuizDto createQuiz(QuizDto quizDto);
    void deleteQuiz(UUID quizId);
}
