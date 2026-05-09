package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.GiftTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftTransactionRepository extends JpaRepository<GiftTransaction, Long> {

    // Get all gifts sent to a user
    List<GiftTransaction> findByToUserId(String toUserId);

    // Get gifts in a voice channel
    List<GiftTransaction> findByVoiceChannelId(Long voiceChannelId);

}