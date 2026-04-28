package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.ReactionResponseDTO;
import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.entity.MessageReaction;
import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.repository.MessageReactionRepository;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageReactionServiceTest {

    @Mock private MessageReactionRepository reactionRepository;
    @Mock private MessageRepository         messageRepository;
    @Mock private UserRepository            userRepository;

    @InjectMocks
    private MessageReactionService messageReactionService;

    private Message         message;
    private User            user;
    private MessageReaction reaction;

    @BeforeEach
    void setUp() {
        TextChannel channel = new TextChannel();
        channel.setId(1L);

        message = new Message();
        message.setId(1L);
        message.setContent("Hello");
        message.setChannel(channel);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("user-keycloak-id");
        user.setFirstName("Test");
        user.setLastName("User");

        reaction = new MessageReaction();
        reaction.setId(1L);
        reaction.setMessage(message);
        reaction.setUser(user);
        reaction.setEmoji("❤️");
    }

    // ── toggleReaction — add ──────────────────────────────────────────────────

    @Test
    void toggleReaction_shouldAddReaction_whenNotExists() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(reactionRepository.findByMessageIdAndUserKeycloakIdAndEmoji(1L, "user-keycloak-id", "❤️"))
                .thenReturn(Optional.empty());
        when(reactionRepository.save(any(MessageReaction.class))).thenReturn(reaction);
        when(reactionRepository.findByMessageId(1L)).thenReturn(List.of(reaction));

        ReactionResponseDTO result = messageReactionService.toggleReaction(1L, "❤️", "user-keycloak-id");

        assertNotNull(result);
        verify(reactionRepository, times(1)).save(any(MessageReaction.class));
        verify(reactionRepository, never()).delete(any());
    }

    // ── toggleReaction — remove ───────────────────────────────────────────────

    @Test
    void toggleReaction_shouldRemoveReaction_whenAlreadyExists() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(reactionRepository.findByMessageIdAndUserKeycloakIdAndEmoji(1L, "user-keycloak-id", "❤️"))
                .thenReturn(Optional.of(reaction));
        when(reactionRepository.findByMessageId(1L)).thenReturn(List.of());

        messageReactionService.toggleReaction(1L, "❤️", "user-keycloak-id");

        verify(reactionRepository, times(1)).delete(reaction);
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void toggleReaction_shouldThrowException_whenMessageNotFound() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                messageReactionService.toggleReaction(99L, "❤️", "user-keycloak-id")
        );
    }

    @Test
    void toggleReaction_shouldThrowException_whenUserNotFound() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                messageReactionService.toggleReaction(1L, "❤️", "unknown")
        );
    }

    // ── getReactions ──────────────────────────────────────────────────────────

    @Test
    void getReactions_shouldReturnReactionResponse() {
        when(reactionRepository.findByMessageId(1L)).thenReturn(List.of(reaction));

        ReactionResponseDTO result = messageReactionService.getReactions(1L, "user-keycloak-id");

        assertNotNull(result);
        assertEquals(1L, result.getMessageId());
    }

    @Test
    void getReactions_shouldReturnEmptyReactions_whenNoReactions() {
        when(reactionRepository.findByMessageId(1L)).thenReturn(List.of());

        ReactionResponseDTO result = messageReactionService.getReactions(1L, "user-keycloak-id");

        assertNotNull(result);
        assertTrue(result.getCounts().isEmpty());
    }

    @Test
    void getReactions_shouldCountReactionsCorrectly() {
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setKeycloakId("user2-keycloak-id");

        MessageReaction reaction2 = new MessageReaction();
        reaction2.setId(2L);
        reaction2.setMessage(message);
        reaction2.setUser(user2);   // ← different user
        reaction2.setEmoji("❤️");

        when(reactionRepository.findByMessageId(1L)).thenReturn(List.of(reaction, reaction2));

        ReactionResponseDTO result = messageReactionService.getReactions(1L, "user-keycloak-id");

        assertEquals(2L, result.getCounts().get("❤️"));
    }
    // ── getReactionsForChannel ────────────────────────────────────────────────

    @Test
    void getReactionsForChannel_shouldReturnMapForAllMessages() {
        when(reactionRepository.findByMessageId(1L)).thenReturn(List.of(reaction));
        when(reactionRepository.findByMessageId(2L)).thenReturn(List.of());

        Map<Long, ReactionResponseDTO> result =
                messageReactionService.getReactionsForChannel(List.of(1L, 2L), "user-keycloak-id");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(1L));
        assertTrue(result.containsKey(2L));
    }

    @Test
    void getReactionsForChannel_shouldReturnEmptyMap_whenNoMessages() {
        Map<Long, ReactionResponseDTO> result =
                messageReactionService.getReactionsForChannel(List.of(), "user-keycloak-id");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReactionsForChannel_shouldTrackUserReaction() {
        when(reactionRepository.findByMessageId(1L)).thenReturn(List.of(reaction));

        Map<Long, ReactionResponseDTO> result =
                messageReactionService.getReactionsForChannel(List.of(1L), "user-keycloak-id");

        assertTrue(result.get(1L).getReacted().get("❤️"));
    }
}