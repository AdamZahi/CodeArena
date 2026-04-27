package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.GiftEventDTO;
import com.codearena.module9_arenatalk.dto.SendGiftRequestDTO;
import com.codearena.module9_arenatalk.entity.GiftTransaction;
import com.codearena.module9_arenatalk.repository.GiftTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GiftService {

    private final GiftTransactionRepository giftRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ArenaTalkWalletService walletService;

    public GiftTransaction sendGift(SendGiftRequestDTO request) {

        // 1. Transfer coins between wallets
        walletService.transferCoins(
                request.getFromUserId(),
                request.getFromUserName(),
                request.getToUserId(),
                request.getToUserName(),
                request.getCoins()
        );

        // 2. Save gift in DB
        GiftTransaction gift = GiftTransaction.builder()
                .fromUserId(request.getFromUserId())
                .fromUserName(request.getFromUserName())
                .toUserId(request.getToUserId())
                .toUserName(request.getToUserName())
                .hubId(request.getHubId())
                .voiceChannelId(request.getVoiceChannelId())
                .giftType(request.getGiftType())
                .coins(request.getCoins())
                .amountMoney(request.getAmountMoney())
                .currency(request.getCurrency())
                .build();

        GiftTransaction savedGift = giftRepository.save(gift);

        // 3. Create WebSocket gift event
        GiftEventDTO event = GiftEventDTO.builder()
                .giftId(savedGift.getId())
                .fromUserId(savedGift.getFromUserId())
                .fromUserName(savedGift.getFromUserName())
                .toUserId(savedGift.getToUserId())
                .toUserName(savedGift.getToUserName())
                .hubId(savedGift.getHubId())
                .voiceChannelId(savedGift.getVoiceChannelId())
                .giftType(savedGift.getGiftType())
                .coins(savedGift.getCoins())
                .message(savedGift.getFromUserName() + " sent " + savedGift.getCoins() + " coins 🎁")
                .build();

        // 4. Send event to everyone in same voice channel
        messagingTemplate.convertAndSend(
                "/topic/voice-gifts/" + savedGift.getVoiceChannelId(),
                event
        );

        return savedGift;
    }
}