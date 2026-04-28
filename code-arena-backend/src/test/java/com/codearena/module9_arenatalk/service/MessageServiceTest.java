package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.MessageRequestDTO;
import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import com.codearena.module9_arenatalk.repository.TextChannelRepository;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository       messageRepository;
    @Mock private TextChannelRepository   textChannelRepository;
    @Mock private UserRepository          userRepository;
    @Mock private UserService             userService;

    @InjectMocks
    private MessageService messageService;

    private TextChannel channel;
    private Message     message;

    @BeforeEach
    void setUp() {
        channel = new TextChannel();
        channel.setId(1L);
        channel.setName("general");

        message = new Message();
        message.setId(1L);
        message.setContent("Hello everyone");
        message.setSenderName("Test User");
        message.setChannel(channel);
        message.setPinned(false);
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Test
    void sendMessage_shouldSaveMessage_whenChannelExists() {
        MessageRequestDTO request = new MessageRequestDTO();
        request.setContent("Hello everyone");

        when(textChannelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        Message result = messageService.sendMessage(1L, request);

        assertNotNull(result);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void sendMessage_shouldThrowException_whenChannelNotFound() {
        MessageRequestDTO request = new MessageRequestDTO();
        request.setContent("Hello");

        when(textChannelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                messageService.sendMessage(99L, request)
        );
    }

    @Test
    void sendMessage_shouldTrimContent_beforeSaving() {
        MessageRequestDTO request = new MessageRequestDTO();
        request.setContent("  Hello   ");

        when(textChannelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            assertEquals("Hello", m.getContent());
            return m;
        });

        messageService.sendMessage(1L, request);
        verify(messageRepository, times(1)).save(any());
    }

    // ── getMessagesByChannel ──────────────────────────────────────────────────

    @Test
    void getMessagesByChannel_shouldReturnMessages() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L))
                .thenReturn(List.of(message));

        List<Message> result = messageService.getMessagesByChannel(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Hello everyone", result.get(0).getContent());
    }

    @Test
    void getMessagesByChannel_shouldReturnEmptyList_whenNoMessages() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L))
                .thenReturn(List.of());

        List<Message> result = messageService.getMessagesByChannel(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── getPinnedMessages ─────────────────────────────────────────────────────

    @Test
    void getPinnedMessagesByChannel_shouldReturnPinnedMessages() {
        message.setPinned(true);
        when(messageRepository.findByChannelIdAndPinnedTrueOrderByPinnedAtDesc(1L))
                .thenReturn(List.of(message));

        List<Message> result = messageService.getPinnedMessagesByChannel(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isPinned());
    }

    // ── deleteMessage ─────────────────────────────────────────────────────────

    @Test
    void deleteMessage_shouldCallDeleteById() {
        doNothing().when(messageRepository).deleteById(1L);

        messageService.deleteMessage(1L);

        verify(messageRepository, times(1)).deleteById(1L);
    }

    // ── updateMessage ─────────────────────────────────────────────────────────

    @Test
    void updateMessage_shouldUpdateContent() {
        Message updated = new Message();
        updated.setContent("Updated content");

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Message result = messageService.updateMessage(1L, updated);

        assertEquals("Updated content", result.getContent());
    }

    @Test
    void updateMessage_shouldThrowException_whenMessageNotFound() {
        Message updated = new Message();
        updated.setContent("New content");

        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                messageService.updateMessage(99L, updated)
        );
    }

    @Test
    void updateMessage_shouldTrimContent() {
        Message updated = new Message();
        updated.setContent("  New content   ");

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Message result = messageService.updateMessage(1L, updated);

        assertEquals("New content", result.getContent());
    }

    // ── pinMessage ────────────────────────────────────────────────────────────

    @Test
    void pinMessage_shouldSetPinnedToTrue() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Message result = messageService.pinMessage(1L);

        assertTrue(result.isPinned());
        assertNotNull(result.getPinnedAt());
    }

    @Test
    void pinMessage_shouldThrowException_whenMessageNotFound() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                messageService.pinMessage(99L)
        );
    }

    // ── unpinMessage ──────────────────────────────────────────────────────────

    @Test
    void unpinMessage_shouldSetPinnedToFalse() {
        message.setPinned(true);

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Message result = messageService.unpinMessage(1L);

        assertFalse(result.isPinned());
        assertNull(result.getPinnedAt());
    }

    @Test
    void unpinMessage_shouldThrowException_whenMessageNotFound() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                messageService.unpinMessage(99L)
        );
    }
}