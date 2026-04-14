package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.MessageRequestDTO;
import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import com.codearena.module9_arenatalk.repository.TextChannelRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final TextChannelRepository textChannelRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public Message sendMessage(Long channelId, MessageRequestDTO request) {
        TextChannel channel = textChannelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        String sender = getCurrentSenderName();

        Message message = Message.builder()
                .content(request.getContent().trim())
                .senderName(sender)
                .channel(channel)
                .pinned(false)
                .build();

        return messageRepository.save(message);
    }

    public List<Message> getMessagesByChannel(Long channelId) {
        return messageRepository.findByChannelIdOrderBySentAtAsc(channelId);
    }

    public List<Message> getPinnedMessagesByChannel(Long channelId) {
        return messageRepository.findByChannelIdAndPinnedTrueOrderByPinnedAtDesc(channelId);
    }

    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    public Message updateMessage(Long messageId, Message updatedMessage) {
        Message existing = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        existing.setContent(updatedMessage.getContent().trim());
        return messageRepository.save(existing);
    }

    public Message pinMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setPinned(true);
        message.setPinnedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public Message unpinMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setPinned(false);
        message.setPinnedAt(null);
        return messageRepository.save(message);
    }

    private String getCurrentSenderName() {
        Jwt jwt = getCurrentJwtOrNull();

        if (jwt == null) {
            return "Unknown User";
        }

        userService.syncFromJwt(jwt);

        String subject = jwt.getSubject();
        User user = userRepository.findByKeycloakId(subject).orElse(null);

        if (user != null) {
            String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
            String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
            String fullName = (firstName + " " + lastName).trim();

            if (!fullName.isEmpty()) {
                return fullName;
            }

            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                return user.getEmail();
            }
        }

        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }

        return "You";
    }

    private Jwt getCurrentJwtOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }

        return null;
    }
}