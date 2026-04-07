package com.codearena.module7_coaching.service;

import com.codearena.module7_coaching.dto.*;

import java.util.List;
import java.util.UUID;

public interface CoachingService {
    // ─── Coaches ───
    List<CoachDto> getAllCoaches();

    CoachDto getCoachById(UUID coachId);

    // ─── Sessions ───
    List<CoachingSessionDto> getAllSessions();

    CoachingSessionDto getSessionById(UUID sessionId);

    CoachingSessionDto createSession(CoachingSessionDto dto);

    void deleteSession(UUID sessionId);

    void rejectSession(UUID sessionId, String coachId);

    // ─── Reservations ───
    CoachingSessionDto bookSession(String userId, BookSessionRequest request);

    void cancelReservation(String userId, UUID sessionId);

    List<CoachingSessionDto> getUserReservations(String userId);

    void sendMeetingLinks(UUID sessionId, String coachId);

    // ─── Recommendations ───
    List<CoachingSessionDto> getRecommendedSessions(String userId);

    // ─── Feedback ───
    void submitFeedback(String userId, SessionFeedbackDto dto);

    List<SessionFeedbackDto> getCoachFeedbacks(String coachId);

    // ─── Dashboard ───
    DashboardDto getUserDashboard(String userId);

    // ─── Notifications ───
    List<NotificationDto> getUserNotifications(String userId);

    void markNotificationRead(UUID notificationId);
}
