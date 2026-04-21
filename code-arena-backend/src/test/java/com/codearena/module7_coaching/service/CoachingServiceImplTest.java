package com.codearena.module7_coaching.service;

import com.codearena.module7_coaching.dto.BookSessionRequest;
import com.codearena.module7_coaching.dto.CoachingSessionDto;
import com.codearena.module7_coaching.entity.CoachingSession;
import com.codearena.module7_coaching.entity.SessionReservation;
import com.codearena.module7_coaching.enums.ProgrammingLanguage;
import com.codearena.module7_coaching.enums.SessionStatus;
import com.codearena.module7_coaching.enums.SkillLevel;
import com.codearena.module7_coaching.repository.*;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CoachingServiceImplTest {

    @Mock private CoachRepository coachRepository;
    @Mock private CoachingSessionRepository sessionRepository;
    @Mock private SessionReservationRepository reservationRepository;
    @Mock private SessionFeedbackRepository feedbackRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserBadgeRepository userBadgeRepository;
    @Mock private CoachingBadgeRepository badgeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private CoachingServiceImpl coachingService;

    private UUID sessionId;
    private String userId;
    private CoachingSession session;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId = "user-123";
        session = CoachingSession.builder()
                .id(sessionId)
                .title("Java Mastery")
                .coachId("coach-456")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .maxParticipants(5)
                .currentParticipants(2)
                .status(SessionStatus.SCHEDULED)
                .language(ProgrammingLanguage.JAVA)
                .level(SkillLevel.INTERMEDIAIRE)
                .build();
    }

    @Test
    void createSession_ShouldThrowException_WhenDateInPast() {
        // Arrange
        CoachingSessionDto dto = CoachingSessionDto.builder()
                .title("Past Session")
                .scheduledAt(LocalDateTime.now().minusHours(1))
                .build();

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            coachingService.createSession(dto);
        });

        assertEquals("Une session ne peut pas être créée dans le passé.", exception.getMessage());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void bookSession_ShouldSucceed_WhenAvailable() {
        // Arrange
        BookSessionRequest request = new BookSessionRequest();
        request.setSessionId(sessionId);

        com.codearena.module7_coaching.entity.CoachingBadge badge = com.codearena.module7_coaching.entity.CoachingBadge.builder()
                .id(UUID.randomUUID())
                .name("Premier Coaching")
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(reservationRepository.existsBySessionIdAndUserIdAndCancelledFalse(sessionId, userId)).thenReturn(false);
        when(sessionRepository.save(any(CoachingSession.class))).thenReturn(session);
        when(badgeRepository.findByName("Premier Coaching")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.existsByUserIdAndBadgeId(eq(userId), any(UUID.class))).thenReturn(false);

        // Act
        CoachingSessionDto result = coachingService.bookSession(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(3, session.getCurrentParticipants());
        verify(reservationRepository, times(1)).save(any(SessionReservation.class));
        verify(notificationRepository, atLeastOnce()).save(any());
    }

    @Test
    void bookSession_ShouldThrowException_WhenSessionFull() {
        // Arrange
        session.setCurrentParticipants(5); // session is full
        BookSessionRequest request = new BookSessionRequest();
        request.setSessionId(sessionId);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            coachingService.bookSession(userId, request);
        });

        assertEquals("Session is full. Max participants reached.", exception.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void bookSession_ShouldThrowException_WhenAlreadyBooked() {
        // Arrange
        BookSessionRequest request = new BookSessionRequest();
        request.setSessionId(sessionId);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(reservationRepository.existsBySessionIdAndUserIdAndCancelledFalse(sessionId, userId)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            coachingService.bookSession(userId, request);
        });

        assertEquals("You have already booked this session.", exception.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_ShouldDecrementParticipantCount() {
        // Arrange
        SessionReservation reservation = SessionReservation.builder()
                .sessionId(sessionId)
                .userId(userId)
                .cancelled(false)
                .build();

        when(reservationRepository.findBySessionIdAndUserIdAndCancelledFalse(sessionId, userId))
                .thenReturn(Optional.of(reservation));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // Act
        coachingService.cancelReservation(userId, sessionId);

        // Assert
        assertTrue(reservation.getCancelled());
        assertEquals(1, session.getCurrentParticipants());
        assertEquals(SessionStatus.SCHEDULED, session.getStatus());
        verify(reservationRepository, times(1)).save(reservation);
        verify(sessionRepository, times(1)).save(session);
        verify(notificationRepository, times(1)).save(any());
    }
}
