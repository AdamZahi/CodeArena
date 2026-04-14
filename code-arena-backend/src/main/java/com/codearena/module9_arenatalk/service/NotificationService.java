package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.ArenNotification;
import com.codearena.module9_arenatalk.entity.NotificationType;
import com.codearena.module9_arenatalk.repository.NotificationRepository;
import com.codearena.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void createNotification(User user, String hubName, Long hubId, NotificationType type) {
        String message = type == NotificationType.ACCEPTED
                ? "Your request to join " + hubName + " was accepted!"
                : "Your request to join " + hubName + " was rejected.";

        ArenNotification notification = ArenNotification.builder()
                .user(user)
                .message(message)
                .hubName(hubName)
                .hubId(hubId)
                .type(type)
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    public List<ArenNotification> getNotifications(String keycloakId) {
        return notificationRepository.findByUserKeycloakIdOrderByCreatedAtDesc(keycloakId);
    }

    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(String keycloakId) {
        notificationRepository.markAllAsRead(keycloakId);
    }
}