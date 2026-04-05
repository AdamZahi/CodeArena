package com.codearena.module6_event.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service("eventEmailService")
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendInvitationEmail(String to, String eventTitle, 
                                     String eventDate, String location) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("codearenapi@gmail.com");
        message.setTo(to);
        message.setSubject("⭐ VIP Invitation - " + eventTitle);
        message.setText(
            "You have been selected as a TOP 10 player!\n\n" +
            "You are invited to: " + eventTitle + "\n" +
            "Date: " + eventDate + "\n" +
            "Location: " + location + "\n\n" +
            "Login to CodeArena to accept or decline your invitation.\n\n" +
            "CodeArena Team"
        );
        mailSender.send(message);
    }
    
    public void sendRegistrationConfirmationEmail(String to, 
                                                   String eventTitle,
                                                   String qrCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("codearenapi@gmail.com");
        message.setTo(to);
        message.setSubject("✅ Registration Confirmed - " + eventTitle);
        message.setText(
            "Your registration is confirmed!\n\n" +
            "Event: " + eventTitle + "\n" +
            "Your QR Code: " + qrCode + "\n\n" +
            "Show this QR code at the event entrance.\n\n" +
            "CodeArena Team"
        );
        mailSender.send(message);
    }

    public void sendReminderEmail(String to, String eventTitle,
                                   String eventDate, String location,
                                   String qrCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("⏰ REMINDER - " + eventTitle + " starts in 24h!");
            helper.setText("""
                <div style="font-family: Arial, sans-serif;
                            background: #0a0a0f;
                            color: #e2e8f0;
                            padding: 40px;
                            border-radius: 12px;">
                    <h1 style="color: #06b6d4;">⏰ EVENT REMINDER</h1>
                    <h2 style="color: #e2e8f0;">%s</h2>
                    <p style="color: #f59e0b; font-size: 18px;">
                        ⚡ Your event starts in 24 hours!
                    </p>
                    <div style="background: #0d0d15;
                                border: 1px solid #1a1a2e;
                                border-radius: 8px;
                                padding: 20px;
                                margin: 20px 0;">
                        <p>📅 <strong>Date:</strong> %s</p>
                        <p>📍 <strong>Location:</strong> %s</p>
                    </div>
                    <p style="color: #64748b;">
                        Don't forget to bring your QR code!
                    </p>
                    <div style="background: #0d0d15;
                                border: 1px solid #8b5cf6;
                                border-radius: 8px;
                                padding: 12px;
                                font-family: monospace;
                                font-size: 11px;
                                color: #8b5cf6;
                                word-break: break-all;">
                        %s
                    </div>
                    <p style="color: #1a1a2e; margin-top: 32px; font-size: 11px;">
                        CodeArena Team
                    </p>
                </div>
                """.formatted(eventTitle, eventDate, location, qrCode), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send reminder email: {}", e.getMessage());
        }
    }
}
