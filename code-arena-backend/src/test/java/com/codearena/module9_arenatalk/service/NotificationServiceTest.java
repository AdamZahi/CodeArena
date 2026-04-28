package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.ArenNotification;
import com.codearena.module9_arenatalk.entity.NotificationType;
import com.codearena.module9_arenatalk.repository.NotificationRepository;
import com.codearena.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User             user;
    private ArenNotification notification;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("user-keycloak-id");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");

        notification = new ArenNotification();
        notification.setId(1L);
        notification.setUser(user);
        notification.setHubName("Test Hub");
        notification.setHubId(1L);
        notification.setType(NotificationType.ACCEPTED);
        notification.setRead(false);
        notification.setMessage("Your request to join Test Hub was accepted!");
    }

    // ── createNotification ────────────────────────────────────────────────────

    @Test
    void createNotification_shouldSaveNotification_whenAccepted() {
        when(notificationRepository.save(any(ArenNotification.class)))
                .thenReturn(notification);

        notificationService.createNotification(user, "Test Hub", 1L, NotificationType.ACCEPTED);

        verify(notificationRepository, times(1)).save(any(ArenNotification.class));
    }

    @Test
    void createNotification_shouldSaveNotification_whenRejected() {
        when(notificationRepository.save(any(ArenNotification.class)))
                .thenReturn(notification);

        notificationService.createNotification(user, "Test Hub", 1L, NotificationType.REJECTED);

        verify(notificationRepository, times(1)).save(any(ArenNotification.class));
    }

    @Test
    void createNotification_shouldSetAcceptedMessage_whenTypeIsAccepted() {
        when(notificationRepository.save(any(ArenNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.createNotification(user, "Gaming Hub", 1L, NotificationType.ACCEPTED);

        verify(notificationRepository).save(argThat(n ->
                n.getMessage().contains("accepted") && n.getMessage().contains("Gaming Hub")
        ));
    }

    @Test
    void createNotification_shouldSetRejectedMessage_whenTypeIsRejected() {
        when(notificationRepository.save(any(ArenNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.createNotification(user, "Gaming Hub", 1L, NotificationType.REJECTED);

        verify(notificationRepository).save(argThat(n ->
                n.getMessage().contains("rejected") && n.getMessage().contains("Gaming Hub")
        ));
    }

    @Test
    void createNotification_shouldSetReadToFalse() {
        when(notificationRepository.save(any(ArenNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.createNotification(user, "Test Hub", 1L, NotificationType.ACCEPTED);

        verify(notificationRepository).save(argThat(n -> !n.isRead()));
    }

    @Test
    void createNotification_shouldSetCorrectHubId() {
        when(notificationRepository.save(any(ArenNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.createNotification(user, "Test Hub", 42L, NotificationType.ACCEPTED);

        verify(notificationRepository).save(argThat(n -> n.getHubId().equals(42L)));
    }

    // ── getNotifications ──────────────────────────────────────────────────────

    @Test
    void getNotifications_shouldReturnNotifications() {
        when(notificationRepository.findByUserKeycloakIdOrderByCreatedAtDesc("user-keycloak-id"))
                .thenReturn(List.of(notification));

        List<ArenNotification> result = notificationService.getNotifications("user-keycloak-id");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getNotifications_shouldReturnEmptyList_whenNoNotifications() {
        when(notificationRepository.findByUserKeycloakIdOrderByCreatedAtDesc("user-keycloak-id"))
                .thenReturn(List.of());

        List<ArenNotification> result = notificationService.getNotifications("user-keycloak-id");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    @Test
    void markAsRead_shouldSetReadToTrue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(1L);

        assertTrue(notification.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void markAsRead_shouldDoNothing_whenNotificationNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        notificationService.markAsRead(99L);

        verify(notificationRepository, never()).save(any());
    }

    // ── markAllAsRead ─────────────────────────────────────────────────────────

    @Test
    void markAllAsRead_shouldCallRepository() {
        doNothing().when(notificationRepository).markAllAsRead("user-keycloak-id");

        notificationService.markAllAsRead("user-keycloak-id");

        verify(notificationRepository, times(1)).markAllAsRead("user-keycloak-id");
    }
}