package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.ReadReceiptDTO;
import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.entity.MessageReadReceipt;
import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.repository.MessageReadReceiptRepository;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
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
class MessageReadReceiptServiceTest {

    @Mock private MessageReadReceiptRepository receiptRepository;
    @Mock private MessageRepository            messageRepository;
    @Mock private UserRepository               userRepository;

    @InjectMocks
    private MessageReadReceiptService receiptService;

    private User    user;
    private Message message;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("user-keycloak-id");
        user.setFirstName("Test");
        user.setLastName("User");

        TextChannel channel = new TextChannel();
        channel.setId(1L);

        message = new Message();
        message.setId(1L);
        message.setContent("Hello");
        message.setChannel(channel);
    }

    // ── markChannelMessagesAsRead ─────────────────────────────────────────────

    @Test
    void markChannelMessagesAsRead_shouldSaveReceipt_whenNotAlreadyRead() {
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L)).thenReturn(List.of(message));
        when(receiptRepository.findByMessageIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());
        when(receiptRepository.save(any(MessageReadReceipt.class))).thenAnswer(inv -> inv.getArgument(0));

        receiptService.markChannelMessagesAsRead(1L, "user-keycloak-id");

        verify(receiptRepository, times(1)).save(any(MessageReadReceipt.class));
    }

    @Test
    void markChannelMessagesAsRead_shouldNotSaveReceipt_whenAlreadyRead() {
        MessageReadReceipt existing = new MessageReadReceipt();
        existing.setId(1L);

        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L)).thenReturn(List.of(message));
        when(receiptRepository.findByMessageIdAndUserId(1L, user.getId())).thenReturn(Optional.of(existing));

        receiptService.markChannelMessagesAsRead(1L, "user-keycloak-id");

        verify(receiptRepository, never()).save(any());
    }

    @Test
    void markChannelMessagesAsRead_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                receiptService.markChannelMessagesAsRead(1L, "unknown")
        );
    }

    @Test
    void markChannelMessagesAsRead_shouldDoNothing_whenNoMessages() {
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L)).thenReturn(List.of());

        receiptService.markChannelMessagesAsRead(1L, "user-keycloak-id");

        verify(receiptRepository, never()).save(any());
    }

    @Test
    void markChannelMessagesAsRead_shouldSaveMultipleReceipts_whenMultipleMessages() {
        Message message2 = new Message();
        message2.setId(2L);
        message2.setContent("World");

        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L)).thenReturn(List.of(message, message2));
        when(receiptRepository.findByMessageIdAndUserId(anyLong(), any())).thenReturn(Optional.empty());
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        receiptService.markChannelMessagesAsRead(1L, "user-keycloak-id");

        verify(receiptRepository, times(2)).save(any());
    }

    // ── getReadStatus ─────────────────────────────────────────────────────────

    @Test
    void getReadStatus_shouldReturnReadStatus_whenUserFound() {
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(receiptRepository.countByMessageId(1L)).thenReturn(3L);
        when(receiptRepository.findByMessageIdAndUserId(1L, user.getId()))
                .thenReturn(Optional.of(new MessageReadReceipt()));

        ReadReceiptDTO result = receiptService.getReadStatus(1L, "user-keycloak-id");

        assertNotNull(result);
        assertEquals(1L, result.getMessageId());
        assertEquals(3L, result.getReadCount());
        assertTrue(result.isReadByCurrentUser());
    }

    @Test
    void getReadStatus_shouldReturnFalse_whenUserHasNotRead() {
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(receiptRepository.countByMessageId(1L)).thenReturn(2L);
        when(receiptRepository.findByMessageIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        ReadReceiptDTO result = receiptService.getReadStatus(1L, "user-keycloak-id");

        assertFalse(result.isReadByCurrentUser());
        assertEquals(2L, result.getReadCount());
    }

    @Test
    void getReadStatus_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                receiptService.getReadStatus(1L, "unknown")
        );
    }

    @Test
    void getReadStatus_shouldReturnZeroCount_whenNoReaders() {
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(receiptRepository.countByMessageId(1L)).thenReturn(0L);
        when(receiptRepository.findByMessageIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        ReadReceiptDTO result = receiptService.getReadStatus(1L, "user-keycloak-id");

        assertEquals(0L, result.getReadCount());
        assertFalse(result.isReadByCurrentUser());
    }
}