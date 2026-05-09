package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChannelIdOrderBySentAtAsc(Long channelId);
    List<Message> findByChannelIdAndPinnedTrueOrderByPinnedAtDesc(Long channelId);
}