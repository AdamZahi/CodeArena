package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.ReadReceiptDTO;
import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.entity.MessageReadReceipt;
import com.codearena.module9_arenatalk.repository.MessageReadReceiptRepository;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageReadReceiptService {

    private final MessageReadReceiptRepository receiptRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void markChannelMessagesAsRead(Long channelId, String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Message> messages = messageRepository.findByChannelIdOrderBySentAtAsc(channelId);

        for (Message message : messages) {
            boolean exists = receiptRepository.findByMessageIdAndUserId(message.getId(), user.getId()).isPresent();

            if (!exists) {
                MessageReadReceipt receipt = MessageReadReceipt.builder()
                        .message(message)
                        .user(user)
                        .readAt(LocalDateTime.now())
                        .build();
                receiptRepository.save(receipt);
            }
        }
    }

    public ReadReceiptDTO getReadStatus(Long messageId, String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long count = receiptRepository.countByMessageId(messageId);
        boolean readByCurrentUser = receiptRepository.findByMessageIdAndUserId(messageId, user.getId()).isPresent();

        return ReadReceiptDTO.builder()
                .messageId(messageId)
                .readCount(count)
                .readByCurrentUser(readByCurrentUser)
                .build();
    }
}