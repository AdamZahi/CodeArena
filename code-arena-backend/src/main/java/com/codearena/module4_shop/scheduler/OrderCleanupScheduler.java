package com.codearena.module4_shop.scheduler;

import com.codearena.module4_shop.entity.Purchase;
import com.codearena.module4_shop.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupScheduler {

    private final PurchaseRepository purchaseRepository;

    // ── ADDED: WebSocket sender to notify admin when cleanup runs ──
    private final SimpMessagingTemplate messagingTemplate;

    // ── AUTO CLEANUP CANCELLED ORDERS ─────────────
    // Runs every day at midnight
    // Deletes CANCELLED orders older than 7 days
    // Keeps DB clean and prevents data bloat
    //
    // Cron expression: "0 0 0 * * *"
    // ┌─ second (0)
    // ├─ minute (0)
    // ├─ hour (0 = midnight)
    // ├─ day of month (* = every day)
    // ├─ month (* = every month)
    // └─ day of week (* = every day)
    //
    // For TESTING: use "0 */1 * * * *" = every minute
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupCancelledOrders() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        // Only delete orders cancelled MORE than 7 days ago
        // Recent cancellations are kept for customer reference
        List<Purchase> oldCancelledOrders =
                purchaseRepository.findCancelledOrdersBefore(cutoffDate);

        if (oldCancelledOrders.isEmpty()) {
            log.info("🧹 Cleanup: No cancelled orders older than 7 days found");
            return;
        }

        // Save count before deleting — needed for WebSocket notification
        int count = oldCancelledOrders.size();

        purchaseRepository.deleteAll(oldCancelledOrders);
        log.info("🧹 Cleanup: Deleted {} cancelled orders older than 7 days", count);

        // ── WEBSOCKET NOTIFICATION TO ADMIN ───────────
        // After cleanup runs → notify admin in real time
        // Admin panel subscribes to /topic/admin/cleanup
        // and shows a toast: "🧹 3 old cancelled orders were deleted"
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CLEANUP_DONE");
        notification.put("deletedCount", count);
        notification.put("message", "🧹 Auto-cleanup: " + count +
                " cancelled orders older than 7 days were deleted.");
        notification.put("timestamp", LocalDateTime.now().toString());

        // Send to admin topic — only admin sees this
        messagingTemplate.convertAndSend("/topic/admin/cleanup", notification);
        log.info("🧹 Cleanup notification sent to admin via WebSocket");
    }

    // ── MANUAL TRIGGER FOR TESTING ────────────────
    // Runs every 5 minutes in dev — change to daily in production
    // Comment this out and uncomment the daily one above for production
    // @Scheduled(fixedDelay = 300000) // every 5 minutes
    // public void cleanupForTesting() { cleanupCancelledOrders(); }
}
