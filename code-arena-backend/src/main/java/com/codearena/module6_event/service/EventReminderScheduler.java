package com.codearena.module6_event.service;

import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventStatus;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventReminderScheduler {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EmailService emailService;

    // Runs every hour to check for events starting in 24 hours
    @Scheduled(fixedRate = 3600000)
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24h = now.plusHours(24);
        LocalDateTime in25h = now.plusHours(25);

        // Find events starting between 24h and 25h from now
        List<ProgrammingEvent> upcomingEvents = eventRepository
            .findByStartDateBetween(in24h, in25h);

        for (ProgrammingEvent event : upcomingEvents) {
            // Get all CONFIRMED registrations for this event
            List<EventRegistration> registrations = 
                registrationRepository.findByEvent_IdAndStatus(
                    event.getId(), EventStatus.CONFIRMED
                );

            log.info("Sending reminders for event: {} to {} participants",
                event.getTitle(), registrations.size());

            for (EventRegistration registration : registrations) {
                try {
                    emailService.sendReminderEmail(
                        "ness09358@gmail.com", // hardcoded for now
                        event.getTitle(),
                        event.getStartDate().toString(),
                        event.getLocation() != null ? 
                            event.getLocation() : "CODEARENA HQ",
                        registration.getQrCode()
                    );
                } catch (Exception e) {
                    log.error("Failed to send reminder: {}", e.getMessage());
                }
            }
        }
    }
}
