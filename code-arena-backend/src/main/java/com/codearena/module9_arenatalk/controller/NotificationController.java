package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.entity.ArenNotification;
import com.codearena.module9_arenatalk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/arenatalk/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<ArenNotification>> getNotifications(@RequestParam String keycloakId) {
        return ResponseEntity.ok(notificationService.getNotifications(keycloakId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam String keycloakId) {
        notificationService.markAllAsRead(keycloakId);
        return ResponseEntity.noContent().build();
    }
}