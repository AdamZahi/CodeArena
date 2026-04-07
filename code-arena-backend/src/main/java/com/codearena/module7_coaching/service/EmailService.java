package com.codearena.module7_coaching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendMeetingLinks(String sessionTitle, String scheduledAt, String meetingUrl, List<String> toEmails) {
        if (toEmails == null || toEmails.isEmpty()) {
            log.warn("sendMeetingLinks: No recipients provided for session {}", sessionTitle);
            return;
        }

        String subject = "CodeArena Coaching: " + sessionTitle;
        String body = String.format(
                "Hello,\n\nHere is the Google Meet link for our upcoming coaching session (\"%s\") scheduled for %s.\n\n"
                        +
                        "Join the meeting here: %s\n\nBest regards,\nCodeArena & Your Coach",
                sessionTitle, scheduledAt, meetingUrl != null ? meetingUrl : "No link provided by coach yet.");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(fromEmail); // Always send to self
        message.setBcc(toEmails.toArray(new String[0])); // BCC all participants
        message.setSubject(subject);
        message.setText(body);

        try {
            javaMailSender.send(message);
            log.info("sendMeetingLinks: Successfully sent email to {} participants for session {}", toEmails.size(),
                    sessionTitle);
        } catch (Exception e) {
            log.error("sendMeetingLinks: Failed to send emails", e);
            throw new RuntimeException(
                    "Échec de l'envoi de l'e-mail. Vérifiez les paramètres SMTP : " + e.getMessage());
        }
    }

    public void sendCancellationEmail(String sessionTitle, String scheduledAt, String reason, List<String> toEmails) {
        if (toEmails == null || toEmails.isEmpty())
            return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setBcc(toEmails.toArray(new String[0]));
        message.setSubject("CodeArena: Session Cancelled - " + sessionTitle);
        message.setText(String.format(
                "Hello,\n\nWe regret to inform you that the coaching session \"%s\" scheduled for %s has been cancelled by the coach.\n\n"
                        +
                        "Reason: %s\n\nYour spot has been released. We apologize for the inconvenience.\n\nBest regards,\nCodeArena Team",
                sessionTitle, scheduledAt, reason != null ? reason : "Change in schedule"));

        try {
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send cancellation emails", e);
        }
    }

    public void sendApplicationDecisionEmail(String applicantName, String toEmail, boolean approved, String adminNote) {
        if (toEmail == null || toEmail.isBlank()) return;

        String status = approved ? "APPROVED ✅" : "REJECTED ❌";
        String subject = "CodeArena - Coach Application " + status;
        String body = String.format(
                "Hello %s,\n\nYour coach application on CodeArena has been %s by the administrator.\n\n%s%s\n\nBest regards,\nCodeArena Admin Team",
                applicantName,
                status,
                approved ? "Welcome to the coaching team! You can now create sessions and mentor learners.\nLog in to access your Coach Dashboard.\n\n"
                        : "Unfortunately, your application did not meet our requirements at this time.\nYou may reapply after improving your qualifications.\n\n",
                adminNote != null && !adminNote.isBlank() ? "Admin note: " + adminNote + "\n" : "");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            javaMailSender.send(message);
            log.info("Application decision email sent to {} ({})", applicantName, toEmail);
        } catch (Exception e) {
            log.error("Failed to send application decision email to {}", toEmail, e);
        }
    }
}
