package com.codearena.module7_coaching.service;

import com.codearena.module7_coaching.dto.*;
import com.codearena.module7_coaching.entity.*;
import com.codearena.module7_coaching.enums.*;
import com.codearena.module7_coaching.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AnswerRepository answerRepository;
    private final UserSkillRepository userSkillRepository;
    private final NotificationRepository notificationRepository;

    // ─── Score thresholds for classification ───
    private static final double BASIQUE_THRESHOLD = 40.0;
    private static final double INTERMEDIAIRE_THRESHOLD = 70.0;

    @Override
    public List<QuizDto> getAllQuizzes() {
        return quizRepository.findAll().stream()
                .map(this::toQuizDto)
                .collect(Collectors.toList());
    }

    @Override
    public QuizDto getQuizById(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found: " + quizId));
        QuizDto dto = toQuizDto(quiz);
        // Load questions for the quiz detail view
        List<QuestionDto> questions = questionRepository.findByQuizId(quizId).stream()
                .map(this::toQuestionDto)
                .collect(Collectors.toList());
        dto.setQuestions(questions);
        return dto;
    }

    @Override
    @Transactional
    public QuizResultDto submitQuiz(String userId, SubmitQuizRequest request) {
        UUID quizId = request.getQuizId();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found: " + quizId));

        List<Question> questions = questionRepository.findByQuizId(quizId);
        Map<UUID, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // Create the attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(quizId)
                .userId(userId)
                .build();
        attempt = quizAttemptRepository.save(attempt);

        int totalScore = 0;
        int totalPoints = 0;
        List<AnswerResultDto> answerResults = new ArrayList<>();
        Map<ProgrammingLanguage, int[]> languageScores = new HashMap<>(); // [earned, total]

        for (Map.Entry<UUID, String> entry : request.getAnswers().entrySet()) {
            UUID questionId = entry.getKey();
            String userAnswer = entry.getValue();
            Question question = questionMap.get(questionId);

            if (question == null)
                continue;

            boolean isCorrect = question.getCorrectAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            int earned = isCorrect ? question.getPoints() : 0;
            totalScore += earned;
            totalPoints += question.getPoints();

            // Track per-language scores
            languageScores.computeIfAbsent(question.getLanguage(), k -> new int[] { 0, 0 });
            languageScores.get(question.getLanguage())[0] += earned;
            languageScores.get(question.getLanguage())[1] += question.getPoints();

            // Save individual answer
            Answer answer = Answer.builder()
                    .questionId(questionId)
                    .quizAttemptId(attempt.getId())
                    .userAnswer(userAnswer)
                    .isCorrect(isCorrect)
                    .build();
            answerRepository.save(answer);

            answerResults.add(AnswerResultDto.builder()
                    .questionId(questionId)
                    .userAnswer(userAnswer)
                    .correctAnswer(question.getCorrectAnswer())
                    .isCorrect(isCorrect)
                    .explanation(question.getExplanation())
                    .points(earned)
                    .build());
        }

        // Calculate percentage and classify level
        double percentage = totalPoints > 0 ? (totalScore * 100.0 / totalPoints) : 0;
        SkillLevel level = classifyLevel(percentage);

        // Detect weak topics (languages where score < 50%)
        List<String> weakTopics = new ArrayList<>();
        for (Map.Entry<ProgrammingLanguage, int[]> entry : languageScores.entrySet()) {
            double langPercent = entry.getValue()[1] > 0
                    ? (entry.getValue()[0] * 100.0 / entry.getValue()[1])
                    : 0;
            if (langPercent < 50) {
                weakTopics.add(entry.getKey().name());
            }
        }

        // Update the attempt
        attempt.setScore(totalScore);
        attempt.setTotalPoints(totalPoints);
        attempt.setLevel(level);
        attempt.setWeakTopics(String.join(",", weakTopics));
        quizAttemptRepository.save(attempt);

        // Update user skills per language
        updateUserSkills(userId, languageScores);

        // Send notification
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .message(String.format("Quiz terminé ! Score: %d/%d (%s). Niveau: %s",
                        totalScore, totalPoints, String.format("%.0f%%", percentage), level.name()))
                .build());

        log.info("User {} completed quiz {} with score {}/{} ({}%) - Level: {}",
                userId, quizId, totalScore, totalPoints, String.format("%.1f", percentage), level);

        return QuizResultDto.builder()
                .attemptId(attempt.getId())
                .quizId(quizId)
                .score(totalScore)
                .totalPoints(totalPoints)
                .percentage(percentage)
                .level(level)
                .weakTopics(weakTopics)
                .answerResults(answerResults)
                .completedAt(attempt.getCompletedAt())
                .build();
    }

    @Override
    public List<QuizResultDto> getUserHistory(String userId) {
        return quizAttemptRepository.findByUserIdOrderByCompletedAtDesc(userId).stream()
                .map(attempt -> {
                    double percentage = attempt.getTotalPoints() > 0
                            ? (attempt.getScore() * 100.0 / attempt.getTotalPoints())
                            : 0;
                    List<String> weakTopicsList = attempt.getWeakTopics() != null && !attempt.getWeakTopics().isEmpty()
                            ? Arrays.asList(attempt.getWeakTopics().split(","))
                            : Collections.emptyList();
                    return QuizResultDto.builder()
                            .attemptId(attempt.getId())
                            .quizId(attempt.getQuizId())
                            .score(attempt.getScore())
                            .totalPoints(attempt.getTotalPoints())
                            .percentage(percentage)
                            .level(attempt.getLevel())
                            .weakTopics(weakTopicsList)
                            .completedAt(attempt.getCompletedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public QuizDto createQuiz(QuizDto quizDto) {
        Quiz quiz = Quiz.builder()
                .title(quizDto.getTitle())
                .description(quizDto.getDescription())
                .difficulty(quizDto.getDifficulty())
                .language(quizDto.getLanguage())
                .totalPoints(quizDto.getTotalPoints() != null ? quizDto.getTotalPoints() : 0)
                .category(quizDto.getCategory() != null ? quizDto.getCategory() : "PROBLEM_SOLVING")
                .createdBy(quizDto.getCreatedBy())
                .build();
        quiz = quizRepository.save(quiz);

        // Save questions if provided
        if (quizDto.getQuestions() != null) {
            int totalPts = 0;
            for (QuestionDto qDto : quizDto.getQuestions()) {
                Question question = Question.builder()
                        .quizId(quiz.getId())
                        .content(qDto.getContent())
                        .type(qDto.getType())
                        .language(qDto.getLanguage())
                        .difficulty(qDto.getDifficulty())
                        .points(qDto.getPoints() != null ? qDto.getPoints() : 10)
                        .correctAnswer(qDto.getCorrectAnswer())
                        .explanation(qDto.getExplanation())
                        .codeSnippet(qDto.getCodeSnippet())
                        .options(qDto.getOptions())
                        .build();
                questionRepository.save(question);
                totalPts += question.getPoints();
            }
            quiz.setTotalPoints(totalPts);
            quizRepository.save(quiz);
        }

        return toQuizDto(quiz);
    }

    @Override
    public void deleteQuiz(UUID quizId) {
        quizRepository.deleteById(quizId);
    }

    // ─── Private helpers ───

    private SkillLevel classifyLevel(double percentage) {
        if (percentage >= INTERMEDIAIRE_THRESHOLD)
            return SkillLevel.AVANCE;
        if (percentage >= BASIQUE_THRESHOLD)
            return SkillLevel.INTERMEDIAIRE;
        return SkillLevel.BASIQUE;
    }

    private void updateUserSkills(String userId, Map<ProgrammingLanguage, int[]> languageScores) {
        for (Map.Entry<ProgrammingLanguage, int[]> entry : languageScores.entrySet()) {
            ProgrammingLanguage lang = entry.getKey();
            double langPercent = entry.getValue()[1] > 0
                    ? (entry.getValue()[0] * 100.0 / entry.getValue()[1])
                    : 0;
            SkillLevel langLevel = classifyLevel(langPercent);

            Optional<UserSkill> existing = userSkillRepository.findByUserIdAndLanguage(userId, lang);
            if (existing.isPresent()) {
                UserSkill skill = existing.get();
                // Running average
                double newAvg = (skill.getScoreAverage() + langPercent) / 2;
                skill.setScoreAverage(newAvg);
                skill.setLevel(classifyLevel(newAvg));
                userSkillRepository.save(skill);
            } else {
                userSkillRepository.save(UserSkill.builder()
                        .userId(userId)
                        .language(lang)
                        .level(langLevel)
                        .scoreAverage(langPercent)
                        .build());
            }
        }
    }

    private QuizDto toQuizDto(Quiz quiz) {
        return QuizDto.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .difficulty(quiz.getDifficulty())
                .language(quiz.getLanguage())
                .totalPoints(quiz.getTotalPoints())
                .category(quiz.getCategory())
                .createdBy(quiz.getCreatedBy())
                .createdAt(quiz.getCreatedAt())
                .build();
    }

    private QuestionDto toQuestionDto(Question q) {
        return QuestionDto.builder()
                .id(q.getId())
                .quizId(q.getQuizId())
                .content(q.getContent())
                .type(q.getType())
                .language(q.getLanguage())
                .difficulty(q.getDifficulty())
                .points(q.getPoints())
                .correctAnswer(q.getCorrectAnswer())
                .explanation(q.getExplanation())
                .codeSnippet(q.getCodeSnippet())
                .options(q.getOptions())
                .build();
    }
}
