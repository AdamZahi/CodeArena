package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.ReactionResponseDTO;
import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.entity.MessageReaction;
import com.codearena.module9_arenatalk.repository.MessageReactionRepository;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageReactionService {

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReactionResponseDTO toggleReaction(Long messageId, String emoji, String keycloakId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Optional<MessageReaction> existing = reactionRepository
                .findByMessageIdAndUserKeycloakIdAndEmoji(messageId, keycloakId, emoji);

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            MessageReaction reaction = MessageReaction.builder()
                    .message(message)
                    .user(user)
                    .emoji(emoji)
                    .build();
            reactionRepository.save(reaction);
        }

        return buildResponse(messageId, keycloakId);
    }

    public ReactionResponseDTO getReactions(Long messageId, String keycloakId) {
        return buildResponse(messageId, keycloakId);
    }

    public Map<Long, ReactionResponseDTO> getReactionsForChannel(List<Long> messageIds, String keycloakId) {
        return messageIds.stream()
                .collect(Collectors.toMap(id -> id, id -> buildResponse(id, keycloakId)));
    }

    private ReactionResponseDTO buildResponse(Long messageId, String keycloakId) {
        List<MessageReaction> reactions = reactionRepository.findByMessageId(messageId);

        Map<String, Long> counts = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji, Collectors.counting()));

        Map<String, Boolean> reacted = reactions.stream()
                .filter(r -> r.getUser().getKeycloakId().equals(keycloakId))
                .collect(Collectors.toMap(MessageReaction::getEmoji, r -> true));

        return new ReactionResponseDTO(messageId, counts, reacted);
    }
}