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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("codearenapi@gmail.com");
            helper.setTo(to);
            helper.setSubject("⭐ VIP INVITATION - " + eventTitle);
            helper.setText("""
                <div style="font-family: Arial, sans-serif;
                            background: #0a0a0f;
                            color: #e2e8f0;
                            padding: 40px;
                            border-radius: 12px;">
                    <h1 style="color: #f59e0b;">⭐ VIP INVITATION</h1>
                    <h2 style="color: #e2e8f0;">%s</h2>
                    <p style="color: #06b6d4; font-size: 18px;">
                        Congratulations! You have been selected as a TOP 10 player.
                    </p>
                    <div style="background: #0d0d15;
                                border: 1px solid #1a1a2e;
                                border-radius: 8px;
                                padding: 20px;
                                margin: 20px 0;">
                        <p>📅 <strong>Date:</strong> %s</p>
                        <p>📍 <strong>Location:</strong> %s</p>
                    </div>
                    <p style="color: #e2e8f0;">
                        Login to CodeArena now to accept or decline your exclusive invitation.
                    </p>
                    <p style="color: #1a1a2e; margin-top: 32px; font-size: 11px;">
                        CodeArena Team
                    </p>
                </div>
                """.formatted(eventTitle, eventDate, location), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send invitation email: {}", e.getMessage());
        }
    }
    
    public void sendRegistrationConfirmationEmail(String to, 
                                                   String eventTitle,
                                                   String eventDate,
                                                   String location) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("codearenapi@gmail.com");
            helper.setTo(to);
            helper.setSubject("✅ REGISTRATION CONFIRMED - " + eventTitle);
            helper.setText("""
                <div style="font-family: Arial, sans-serif;
                            background: #0a0a0f;
                            color: #e2e8f0;
                            padding: 40px;
                            border-radius: 12px;">
                    <h1 style="color: #10b981;">✅ REGISTRATION CONFIRMED</h1>
                    <h2 style="color: #e2e8f0;">%s</h2>
                    <p style="color: #e2e8f0; font-size: 18px;">
                        ⚡ Your spot is secured!
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
                    <p style="color: #1a1a2e; margin-top: 32px; font-size: 11px;">
                        CodeArena Team
                    </p>
                </div>
                """.formatted(eventTitle, eventDate, location), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send registration confirmation email: {}", e.getMessage());
        }
    }

    public void sendReminderEmail(String to, String eventTitle,
                                   String eventDate, String location,
                                   String qrCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("codearenapi@gmail.com");
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
                    <p style="color: #1a1a2e; margin-top: 32px; font-size: 11px;">
                        CodeArena Team
                    </p>
                </div>
                """.formatted(eventTitle, eventDate, location), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send reminder email: {}", e.getMessage());
        }
    }
}
