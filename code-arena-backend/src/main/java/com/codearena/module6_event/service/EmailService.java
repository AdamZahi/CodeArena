package com.codearena.module6_event.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
}
