package com.codearena.module9_arenatalk.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArenatalkEmailServiceTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private ArenatalkEmailService emailService;

    // ── sendHubAcceptedEmail ───────────────────────────────────────────────────

    @Test
    void sendHubAcceptedEmail_shouldSendEmail_whenCalled() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHubAcceptedEmail("test@test.com", "John Doe", "Gaming Hub");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendHubAcceptedEmail_shouldNotThrow_whenMailFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        // Should not throw — exception is caught internally
        emailService.sendHubAcceptedEmail("test@test.com", "John", "Hub");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendHubAcceptedEmail_shouldCreateMimeMessage() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHubAcceptedEmail("user@test.com", "Jane", "Test Hub");

        verify(mailSender, times(1)).createMimeMessage();
    }

    @Test
    void sendHubAcceptedEmail_shouldHandleDifferentHubNames() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHubAcceptedEmail("user@test.com", "User", "My Special Hub");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ── sendHubRejectedEmail ──────────────────────────────────────────────────

    @Test
    void sendHubRejectedEmail_shouldSendEmail_whenCalled() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHubRejectedEmail("test@test.com", "John Doe", "Gaming Hub");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendHubRejectedEmail_shouldNotThrow_whenMailFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        // Should not throw — exception is caught internally
        emailService.sendHubRejectedEmail("test@test.com", "John", "Hub");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendHubRejectedEmail_shouldCreateMimeMessage() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHubRejectedEmail("user@test.com", "Jane", "Test Hub");

        verify(mailSender, times(1)).createMimeMessage();
    }

    @Test
    void sendHubRejectedEmail_shouldHandleDifferentUsers() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHubRejectedEmail("another@test.com", "Another User", "Private Hub");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}