package com.codearena.module7_coaching.service;

import com.codearena.module7_coaching.dto.*;
import com.codearena.module7_coaching.entity.*;
import com.codearena.module7_coaching.enums.*;
import com.codearena.module7_coaching.repository.*;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachingServiceImpl implements CoachingService {

        private final CoachRepository coachRepository;
        private final CoachingSessionRepository sessionRepository;
        private final SessionReservationRepository reservationRepository;
        private final SessionFeedbackRepository feedbackRepository;
        private final UserSkillRepository userSkillRepository;
        private final QuizAttemptRepository quizAttemptRepository;
        private final NotificationRepository notificationRepository;
        private final UserBadgeRepository userBadgeRepository;
        private final CoachingBadgeRepository badgeRepository;
        private final UserRepository userRepository;
        private final EmailService emailService;

        // ═══════════════════════════════════════════
        // COACHES
        // ═══════════════════════════════════════════

        @Override
        public List<CoachDto> getAllCoaches() {
                return userRepository.findByRole(Role.COACH).stream()
                                .map(user -> {
                                        Coach coachProfile = coachRepository.findByUserId(user.getAuth0Id())
                                                        .orElseGet(() -> Coach.builder()
                                                                        .userId(user.getAuth0Id())
                                                                        .bio("Hi! I'm " + (user.getFirstName() != null
                                                                                        ? user.getFirstName()
                                                                                        : "a coach")
                                                                                        + " — a new coach on Code Arena. Update your bio in your profile settings.")
                                                                        .specializations(Collections
                                                                                        .singletonList("JAVA"))
                                                                        .rating(0.0)
                                                                        .totalSessions(0)
                                                                        .build());

                                        // Compute real session count from DB
                                        int realSessionCount = sessionRepository.findByCoachId(user.getAuth0Id())
                                                        .size();

                                        // Compute real rating from feedbacks
                                        List<SessionFeedback> feedbacks = feedbackRepository
                                                        .findByCoachId(user.getAuth0Id());
                                        double realRating = feedbacks.stream()
                                                        .mapToDouble(SessionFeedback::getRating)
                                                        .average()
                                                        .orElse(coachProfile.getRating() != null
                                                                        ? coachProfile.getRating()
                                                                        : 0.0);
                                        realRating = Math.round(realRating * 10.0) / 10.0;

                                        return CoachDto.builder()
                                                        .id(coachProfile.getId() != null ? coachProfile.getId()
                                                                        : UUID.randomUUID())
                                                        .userId(user.getAuth0Id())
                                                        .name(((user.getFirstName() != null ? user.getFirstName() : "")
                                                                        + " "
                                                                        + (user.getLastName() != null
                                                                                        ? user.getLastName()
                                                                                        : ""))
                                                                        .trim())
                                                        .bio(coachProfile.getBio())
                                                        .specializations(coachProfile.getSpecializations() != null
                                                                        && !coachProfile.getSpecializations().isEmpty()
                                                                                        ? coachProfile.getSpecializations()
                                                                                        : Collections.singletonList(
                                                                                                        "JAVA"))
                                                        .rating(realRating)
                                                        .totalSessions(realSessionCount)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Override
        public CoachDto getCoachById(UUID coachId) {
                return coachRepository.findById(coachId)
                                .map(this::toCoachDto)
                                .orElseThrow(() -> new RuntimeException("Coach not found: " + coachId));
        }

        // ═══════════════════════════════════════════
        // SESSIONS
        // ═══════════════════════════════════════════

        @Override
        public List<CoachingSessionDto> getAllSessions() {
                return sessionRepository.findAll().stream()
                                .map(this::toSessionDto)
                                .collect(Collectors.toList());
        }

        @Override
        public CoachingSessionDto getSessionById(UUID sessionId) {
                return sessionRepository.findById(sessionId)
                                .map(this::toSessionDto)
                                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        }

        @Override
        public CoachingSessionDto createSession(CoachingSessionDto dto) {
                if (dto.getScheduledAt() != null && dto.getScheduledAt().isBefore(LocalDateTime.now())) {
                        throw new RuntimeException("Une session ne peut pas être créée dans le passé.");
                }

                CoachingSession session = CoachingSession.builder()
                                .coachId(dto.getCoachId())
                                .learnerId(dto.getLearnerId())
                                .title(dto.getTitle())
                                .description(dto.getDescription())
                                .language(dto.getLanguage())
                                .level(dto.getLevel())
                                .scheduledAt(dto.getScheduledAt())
                                .durationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 60)
                                .status(SessionStatus.SCHEDULED)
                                .meetingUrl(dto.getMeetingUrl())
                                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 10)
                                .currentParticipants(0)
                                .build();
                session = sessionRepository.save(session);
                log.info("Created coaching session: {} ({})", session.getTitle(), session.getId());
                return toSessionDto(session);
        }

        @Override
        @Transactional
        public void deleteSession(UUID sessionId) {
                reservationRepository.deleteBySessionId(sessionId);
                sessionRepository.deleteById(sessionId);
        }

        @Override
        public void rejectSession(UUID sessionId, String coachId) {
                CoachingSession session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> new RuntimeException("Session not found"));

                if (!session.getCoachId().equals(coachId)) {
                        throw new RuntimeException("Unauthorized: You are not the coach for this session");
                }

                // Get participants emails before cancelling
                List<SessionReservation> reservations = reservationRepository.findBySessionId(sessionId).stream()
                                .filter(r -> r.getCancelled() != null && !r.getCancelled())
                                .collect(Collectors.toList());

                List<String> toEmails = reservations.stream()
                                .map(r -> userRepository.findByKeycloakId(r.getUserId()))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(User::getEmail)
                                .filter(e -> e != null && !e.isEmpty())
                                .collect(Collectors.toList());

                // Notify participants
                if (!toEmails.isEmpty()) {
                        emailService.sendCancellationEmail(
                                        session.getTitle(),
                                        session.getScheduledAt().toString(),
                                        "Annulation par le coach",
                                        toEmails);
                }

                // Reset the session to make it open/available again
                session.setStatus(SessionStatus.SCHEDULED);
                session.setCurrentParticipants(0);
                sessionRepository.save(session);

                // Also cancel currently active reservations
                reservations.forEach(r -> r.setCancelled(true));
                reservationRepository.saveAll(reservations);
        }

        // ═══════════════════════════════════════════
        // RESERVATIONS
        // ═══════════════════════════════════════════

        @Override
        @Transactional
        public CoachingSessionDto bookSession(String userId, BookSessionRequest request) {
                CoachingSession session = sessionRepository.findById(request.getSessionId())
                                .orElseThrow(() -> new RuntimeException("Session not found"));

                // Check availability
                if (session.getCurrentParticipants() >= session.getMaxParticipants()) {
                        throw new RuntimeException("Session is full. Max participants reached.");
                }

                // Check if already booked
                if (reservationRepository.existsBySessionIdAndUserIdAndCancelledFalse(session.getId(), userId)) {
                        throw new RuntimeException("You have already booked this session.");
                }

                // Create reservation
                reservationRepository.save(SessionReservation.builder()
                                .sessionId(session.getId())
                                .userId(userId)
                                .build());

                // Update participant count
                session.setCurrentParticipants(session.getCurrentParticipants() + 1);
                if (session.getCurrentParticipants() >= session.getMaxParticipants()) {
                        session.setStatus(SessionStatus.RESERVED);
                }
                sessionRepository.save(session);

                // Send confirmation notification
                notificationRepository.save(Notification.builder()
                                .userId(userId)
                                .message(String.format("✅ Réservation confirmée : %s le %s",
                                                session.getTitle(), session.getScheduledAt()))
                                .build());

                // Check for badge: first session booked
                awardBadgeIfEligible(userId, "Premier Coaching",
                                "Félicitations ! Vous avez réservé votre première session de coaching.");

                log.info("User {} booked session {}", userId, session.getId());
                return toSessionDto(session);
        }

        @Override
        @Transactional
        public void cancelReservation(String userId, UUID sessionId) {
                SessionReservation reservation = reservationRepository
                                .findBySessionIdAndUserIdAndCancelledFalse(sessionId, userId)
                                .orElseThrow(() -> new RuntimeException("Reservation not found"));

                reservation.setCancelled(true);
                reservationRepository.save(reservation);

                CoachingSession session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> new RuntimeException("Session not found"));

                session.setCurrentParticipants(Math.max(0, session.getCurrentParticipants() - 1));
                if (session.getStatus() == SessionStatus.RESERVED) {
                        session.setStatus(SessionStatus.SCHEDULED);
                }
                sessionRepository.save(session);

                notificationRepository.save(Notification.builder()
                                .userId(userId)
                                .message(String.format("❌ Réservation annulée : %s", session.getTitle()))
                                .build());
        }

        @Override
        public List<CoachingSessionDto> getUserReservations(String userId) {
                List<UUID> sessionIds = reservationRepository.findByUserIdAndCancelledFalse(userId).stream()
                                .map(SessionReservation::getSessionId)
                                .collect(Collectors.toList());

                return sessionRepository.findAllById(sessionIds).stream()
                                .map(this::toSessionDto)
                                .collect(Collectors.toList());
        }

        @Override
        public void sendMeetingLinks(UUID sessionId, String coachId) {
                CoachingSession session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> new RuntimeException("Session not found"));

                if (!session.getCoachId().equals(coachId)) {
                        throw new RuntimeException("Unauthorized: You are not the coach for this session");
                }

                // Get all active reservations for this session
                List<SessionReservation> reservations = reservationRepository.findBySessionId(sessionId).stream()
                                .filter(r -> r.getCancelled() != null && !r.getCancelled())
                                .collect(Collectors.toList());

                // Fetch real email for each participant
                List<String> toEmails = reservations.stream()
                                .map(r -> userRepository.findByKeycloakId(r.getUserId()))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(User::getEmail)
                                .filter(email -> email != null && !email.isEmpty())
                                .collect(Collectors.toList());

                if (toEmails.isEmpty()) {
                        throw new RuntimeException(
                                        "Aucun participant n'a d'adresse e-mail enregistrée dans son profil.");
                }

                emailService.sendMeetingLinks(
                                session.getTitle(),
                                session.getScheduledAt().toString(),
                                session.getMeetingUrl(),
                                toEmails);
        }

        // ═══════════════════════════════════════════
        // INTELLIGENT RECOMMENDATION
        // ═══════════════════════════════════════════

        @Override
        public List<CoachingSessionDto> getRecommendedSessions(String userId) {
                List<UserSkill> skills = userSkillRepository.findByUserId(userId);

                if (skills.isEmpty()) {
                        // No skill data yet → return all scheduled sessions
                        return sessionRepository.findByStatus(SessionStatus.SCHEDULED).stream()
                                        .map(this::toSessionDto)
                                        .collect(Collectors.toList());
                }

                // Identify weak languages (BASIQUE level) and build recommendation
                List<ProgrammingLanguage> weakLanguages = skills.stream()
                                .filter(s -> s.getLevel() == SkillLevel.BASIQUE
                                                || s.getLevel() == SkillLevel.INTERMEDIAIRE)
                                .map(UserSkill::getLanguage)
                                .collect(Collectors.toList());

                List<SkillLevel> targetLevels = Arrays.asList(SkillLevel.BASIQUE, SkillLevel.INTERMEDIAIRE);

                if (weakLanguages.isEmpty()) {
                        // User is advanced in everything → recommend advanced sessions
                        weakLanguages = skills.stream().map(UserSkill::getLanguage).collect(Collectors.toList());
                        targetLevels = Collections.singletonList(SkillLevel.AVANCE);
                }

                List<CoachingSession> recommended = sessionRepository
                                .findByLanguageInAndLevelInAndStatus(weakLanguages, targetLevels,
                                                SessionStatus.SCHEDULED);

                // If no specific recommendations, fall back to all scheduled
                if (recommended.isEmpty()) {
                        recommended = sessionRepository.findByStatus(SessionStatus.SCHEDULED);
                }

                return recommended.stream()
                                .filter(s -> s.getCurrentParticipants() < s.getMaxParticipants())
                                .map(this::toSessionDto)
                                .collect(Collectors.toList());
        }

        // ═══════════════════════════════════════════
        // FEEDBACK
        // ═══════════════════════════════════════════

        @Override
        @Transactional
        public void submitFeedback(String userId, SessionFeedbackDto dto) {
                log.info("submitFeedback: saving feedback from user={} for coach={} rating={}",
                                userId, dto.getCoachId(), dto.getRating());

                SessionFeedback feedback = SessionFeedback.builder()
                                .coachId(dto.getCoachId())
                                .userId(userId)
                                .rating(dto.getRating() != null ? dto.getRating() : 0.0)
                                .comment(dto.getComment() != null ? dto.getComment() : "")
                                .build();
                feedbackRepository.save(feedback);
                log.info("submitFeedback: feedback entity saved successfully");

                // Update coach rating — create Coach record if it doesn't exist yet
                Coach coach = coachRepository.findByUserId(dto.getCoachId())
                                .orElse(null);

                if (coach == null) {
                        log.info("Creating coach profile for {} on first feedback", dto.getCoachId());
                        coach = Coach.builder()
                                        .userId(dto.getCoachId())
                                        .bio("Coach on Code Arena.")
                                        .specializations(new ArrayList<>(List.of("GENERAL")))
                                        .rating(0.0)
                                        .totalSessions(0)
                                        .build();
                        coach = coachRepository.save(coach);
                }

                List<SessionFeedback> allFeedbacks = feedbackRepository.findByCoachId(dto.getCoachId());
                double avgRating = allFeedbacks.stream()
                                .mapToDouble(SessionFeedback::getRating)
                                .average()
                                .orElse(0.0);

                // Re-calculate the overall average for this coach (round to 1 decimal)
                coach.setRating(Math.round(avgRating * 10.0) / 10.0);
                coachRepository.save(coach);

                log.info("submitFeedback: coach {} rating updated to {}",
                                dto.getCoachId(), coach.getRating());
        }

        @Override
        public List<SessionFeedbackDto> getCoachFeedbacks(String coachId) {
                List<SessionFeedback> allFeedbacks = feedbackRepository.findByCoachId(coachId);

                return allFeedbacks.stream()
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .map(f -> {
                                        String displayName = "Unknown User";
                                        Optional<User> userOpt = userRepository.findByKeycloakId(f.getUserId());
                                        if (userOpt.isPresent()) {
                                                User u = userOpt.get();
                                                String fName = (u.getFirstName() != null) ? u.getFirstName().trim()
                                                                : "";
                                                String lName = (u.getLastName() != null) ? u.getLastName().trim() : "";

                                                if (!fName.isEmpty() || !lName.isEmpty()) {
                                                        displayName = (fName + " " + lName).trim();
                                                } else if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                                                        displayName = u.getEmail();
                                                }
                                        }

                                        return SessionFeedbackDto.builder()
                                                        .coachId(f.getCoachId())
                                                        .userId(displayName)
                                                        .rating(f.getRating())
                                                        .comment(f.getComment())
                                                        .createdAt(f.getCreatedAt() != null
                                                                        ? f.getCreatedAt().toString()
                                                                        : "")
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        // ═══════════════════════════════════════════
        // DASHBOARD
        // ═══════════════════════════════════════════

        @Override
        public DashboardDto getUserDashboard(String userId) {
                // Quiz stats
                List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByCompletedAtDesc(userId);
                int totalQuizzes = attempts.size();
                double avgScore = attempts.stream()
                                .filter(a -> a.getTotalPoints() > 0)
                                .mapToDouble(a -> a.getScore() * 100.0 / a.getTotalPoints())
                                .average()
                                .orElse(0.0);

                // Overall level from latest attempt
                SkillLevel overallLevel = attempts.isEmpty() ? SkillLevel.BASIQUE : attempts.get(0).getLevel();

                // Skills
                List<UserSkillDto> skills = userSkillRepository.findByUserId(userId).stream()
                                .map(s -> UserSkillDto.builder()
                                                .userId(s.getUserId())
                                                .language(s.getLanguage())
                                                .level(s.getLevel())
                                                .scoreAverage(s.getScoreAverage())
                                                .build())
                                .collect(Collectors.toList());

                // Recommended sessions
                List<CoachingSessionDto> recommended = getRecommendedSessions(userId);

                // Upcoming sessions (reserved by user)
                List<CoachingSessionDto> upcoming = getUserReservations(userId);

                // Badges
                List<BadgeDto> badges = userBadgeRepository.findByUserId(userId).stream()
                                .map(ub -> {
                                        CoachingBadge badge = badgeRepository.findById(ub.getBadgeId()).orElse(null);
                                        if (badge == null)
                                                return null;
                                        return BadgeDto.builder()
                                                        .id(badge.getId())
                                                        .name(badge.getName())
                                                        .description(badge.getDescription())
                                                        .iconUrl(badge.getIconUrl())
                                                        .earnedAt(ub.getEarnedAt())
                                                        .build();
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                // Unread notifications count
                long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);

                return DashboardDto.builder()
                                .totalQuizzesTaken(totalQuizzes)
                                .averageScore(Math.round(avgScore * 100.0) / 100.0)
                                .overallLevel(overallLevel != null ? overallLevel : SkillLevel.BASIQUE)
                                .skills(skills)
                                .recommendedSessions(recommended)
                                .upcomingSessions(upcoming)
                                .badges(badges)
                                .unreadNotifications(unread)
                                .build();
        }

        // ═══════════════════════════════════════════
        // NOTIFICATIONS
        // ═══════════════════════════════════════════

        @Override
        public List<NotificationDto> getUserNotifications(String userId) {
                return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                                .map(n -> NotificationDto.builder()
                                                .id(n.getId())
                                                .message(n.getMessage())
                                                .isRead(n.getIsRead())
                                                .createdAt(n.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public void markNotificationRead(UUID notificationId) {
                notificationRepository.findById(notificationId).ifPresent(n -> {
                        n.setIsRead(true);
                        notificationRepository.save(n);
                });
        }

        // ═══════════════════════════════════════════
        // GAMIFICATION HELPERS
        // ═══════════════════════════════════════════

        private void awardBadgeIfEligible(String userId, String badgeName, String description) {
                CoachingBadge badge = badgeRepository.findByName(badgeName)
                                .orElseGet(() -> badgeRepository.save(CoachingBadge.builder()
                                                .name(badgeName)
                                                .description(description)
                                                .build()));

                if (!userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                        userBadgeRepository.save(UserBadge.builder()
                                        .userId(userId)
                                        .badgeId(badge.getId())
                                        .build());

                        notificationRepository.save(Notification.builder()
                                        .userId(userId)
                                        .message(String.format("🏆 Nouveau badge obtenu : %s", badgeName))
                                        .build());

                        log.info("Badge '{}' awarded to user {}", badgeName, userId);
                }
        }

        // ═══════════════════════════════════════════
        // MAPPERS
        // ═══════════════════════════════════════════

        private CoachDto toCoachDto(Coach coach) {
                String coachName = userRepository.findByKeycloakId(coach.getUserId())
                                .map(u -> (u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : ""))
                                                .trim())
                                .orElse(coach.getUserId());

                return CoachDto.builder()
                                .id(coach.getId())
                                .userId(coach.getUserId())
                                .name(coachName)
                                .bio(coach.getBio())
                                .specializations(coach.getSpecializations())
                                .rating(coach.getRating())
                                .totalSessions(coach.getTotalSessions())
                                .build();
        }

        private CoachingSessionDto toSessionDto(CoachingSession s) {
                return CoachingSessionDto.builder()
                                .id(s.getId())
                                .coachId(s.getCoachId())
                                .learnerId(s.getLearnerId())
                                .title(s.getTitle())
                                .description(s.getDescription())
                                .language(s.getLanguage())
                                .level(s.getLevel())
                                .scheduledAt(s.getScheduledAt())
                                .durationMinutes(s.getDurationMinutes())
                                .status(s.getStatus())
                                .meetingUrl(s.getMeetingUrl())
                                .maxParticipants(s.getMaxParticipants())
                                .currentParticipants(s.getCurrentParticipants())
                                .build();
        }
}
