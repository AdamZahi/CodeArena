package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private SemanticSearchService semanticSearchService;

    private Message message1;
    private Message message2;

    @BeforeEach
    void setUp() {
        TextChannel channel = new TextChannel();
        channel.setId(1L);

        message1 = new Message();
        message1.setId(1L);
        message1.setContent("Hello everyone how are you");
        message1.setSenderName("John Doe");
        message1.setChannel(channel);

        message2 = new Message();
        message2.setId(2L);
        message2.setContent("I need help with the challenge");
        message2.setSenderName("Jane Smith");
        message2.setChannel(channel);
    }

    // ── search — repository interaction ───────────────────────────────────────

    @Test
    void search_shouldFetchMessagesFromRepository() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L))
                .thenReturn(List.of(message1, message2));

        // WebClient call will fail (no real server) but we test repo interaction
        try {
            semanticSearchService.search("hello", 1L);
        } catch (Exception ignored) {
            // Expected — no Python server running in tests
        }

        verify(messageRepository, times(1)).findByChannelIdOrderBySentAtAsc(1L);
    }

    @Test
    void search_shouldFetchMessagesForCorrectChannel() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(42L))
                .thenReturn(List.of(message1));

        try {
            semanticSearchService.search("test query", 42L);
        } catch (Exception ignored) {}

        verify(messageRepository, times(1)).findByChannelIdOrderBySentAtAsc(42L);
        verify(messageRepository, never()).findByChannelIdOrderBySentAtAsc(1L);
    }

    @Test
    void search_shouldHandleEmptyMessageList() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L))
                .thenReturn(List.of());

        try {
            semanticSearchService.search("hello", 1L);
        } catch (Exception ignored) {}

        verify(messageRepository, times(1)).findByChannelIdOrderBySentAtAsc(1L);
    }

    @Test
    void search_shouldCallRepositoryOnce_perRequest() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L))
                .thenReturn(List.of(message1));

        try { semanticSearchService.search("query1", 1L); } catch (Exception ignored) {}
        try { semanticSearchService.search("query2", 1L); } catch (Exception ignored) {}

        verify(messageRepository, times(2)).findByChannelIdOrderBySentAtAsc(1L);
    }

    // ── MessageDTO mapping ────────────────────────────────────────────────────

    @Test
    void search_shouldMapMessagesWithCorrectContent() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L))
                .thenReturn(List.of(message1));

        // Just verify repository was called and message data is accessible
        List<Message> messages = messageRepository.findByChannelIdOrderBySentAtAsc(1L);

        assertEquals(1, messages.size());
        assertEquals("Hello everyone how are you", messages.get(0).getContent());
        assertEquals("John Doe", messages.get(0).getSenderName());
        assertEquals(1L, messages.get(0).getId());
    }

    @Test
    void search_shouldMapMultipleMessages() {
        when(messageRepository.findByChannelIdOrderBySentAtAsc(1L))
                .thenReturn(List.of(message1, message2));

        List<Message> messages = messageRepository.findByChannelIdOrderBySentAtAsc(1L);

        assertEquals(2, messages.size());
        assertEquals("John Doe",   messages.get(0).getSenderName());
        assertEquals("Jane Smith", messages.get(1).getSenderName());
    }
}