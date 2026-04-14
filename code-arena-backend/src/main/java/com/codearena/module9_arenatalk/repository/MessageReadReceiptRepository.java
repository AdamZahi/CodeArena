package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.MessageReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, Long> {
    Optional<MessageReadReceipt> findByMessageIdAndUserId(Long messageId, UUID userId);
    List<MessageReadReceipt> findByMessageId(Long messageId);
    long countByMessageId(Long messageId);
}