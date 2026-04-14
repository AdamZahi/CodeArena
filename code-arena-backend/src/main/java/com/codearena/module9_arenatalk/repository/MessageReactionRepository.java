package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    List<MessageReaction> findByMessageId(Long messageId);
    Optional<MessageReaction> findByMessageIdAndUserKeycloakIdAndEmoji(Long messageId, String keycloakId, String emoji);
    void deleteByMessageIdAndUserKeycloakIdAndEmoji(Long messageId, String keycloakId, String emoji);
}