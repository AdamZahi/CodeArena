package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.SendGiftRequestDTO;
import com.codearena.module9_arenatalk.entity.GiftTransaction;
import com.codearena.module9_arenatalk.entity.GiftType;
import com.codearena.module9_arenatalk.repository.GiftTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftServiceTest {

    @Mock
    private GiftTransactionRepository giftRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ArenaTalkWalletService walletService;

    @InjectMocks
    private GiftService giftService;

    private SendGiftRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new SendGiftRequestDTO();
        request.setFromUserId("sender1");
        request.setFromUserName("Sender Name");
        request.setToUserId("receiver1");
        request.setToUserName("Receiver Name");
        request.setHubId(1L);
        request.setVoiceChannelId(10L);
        request.setGiftType(GiftType.FIRE);
        request.setCoins(10);
        request.setAmountMoney(java.math.BigDecimal.valueOf(1.0));
        request.setCurrency("EUR");
    }

    private GiftTransaction buildSavedGift() {
        GiftTransaction saved = new GiftTransaction();
        saved.setId(1L);
        saved.setFromUserId(request.getFromUserId());
        saved.setFromUserName(request.getFromUserName());
        saved.setToUserId(request.getToUserId());
        saved.setToUserName(request.getToUserName());
        saved.setHubId(request.getHubId());
        saved.setVoiceChannelId(request.getVoiceChannelId());
        saved.setGiftType(request.getGiftType());
        saved.setCoins(request.getCoins());
        saved.setAmountMoney(request.getAmountMoney());
        saved.setCurrency(request.getCurrency());
        return saved;
    }

    // ── sendGift ──────────────────────────────────────────────────────────────

    @Test
    void sendGift_shouldCallTransferCoins() {
        when(giftRepository.save(any())).thenReturn(buildSavedGift());

        giftService.sendGift(request);

        verify(walletService, times(1)).transferCoins(
                eq("sender1"), eq("Sender Name"),
                eq("receiver1"), eq("Receiver Name"),
                eq(10)
        );
    }

    @Test
    void sendGift_shouldSaveGiftTransaction() {
        when(giftRepository.save(any())).thenReturn(buildSavedGift());

        giftService.sendGift(request);

        verify(giftRepository, times(1)).save(any(GiftTransaction.class));
    }

    @Test
    void sendGift_shouldReturnSavedGift() {
        GiftTransaction saved = buildSavedGift();
        when(giftRepository.save(any())).thenReturn(saved);

        GiftTransaction result = giftService.sendGift(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void sendGift_shouldSendWebSocketEvent_toCorrectTopic() {
        GiftTransaction saved = buildSavedGift();
        when(giftRepository.save(any())).thenReturn(saved);

        giftService.sendGift(request);


        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/voice-gifts/10"), (Object) any());
    }

    @Test
    void sendGift_shouldSaveGiftWithCorrectFromUserId() {
        when(giftRepository.save(any())).thenAnswer(inv -> {
            GiftTransaction g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        GiftTransaction result = giftService.sendGift(request);

        assertEquals("sender1", result.getFromUserId());
    }

    @Test
    void sendGift_shouldSaveGiftWithCorrectCoins() {
        when(giftRepository.save(any())).thenAnswer(inv -> {
            GiftTransaction g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        GiftTransaction result = giftService.sendGift(request);

        assertEquals(10, result.getCoins());
    }

    @Test
    void sendGift_shouldSaveGiftWithCorrectGiftType() {
        when(giftRepository.save(any())).thenAnswer(inv -> {
            GiftTransaction g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        GiftTransaction result = giftService.sendGift(request);

        assertEquals(GiftType.FIRE, result.getGiftType());
    }

    @Test
    void sendGift_shouldSaveGiftWithCorrectCurrency() {
        when(giftRepository.save(any())).thenAnswer(inv -> {
            GiftTransaction g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        GiftTransaction result = giftService.sendGift(request);

        assertEquals("EUR", result.getCurrency());
    }

    @Test
    void sendGift_shouldThrowException_whenTransferFails() {
        doThrow(new RuntimeException("Not enough ArenaTalk coins"))
                .when(walletService).transferCoins(any(), any(), any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                giftService.sendGift(request)
        );

        assertEquals("Not enough ArenaTalk coins", ex.getMessage());
    }

    @Test
    void sendGift_shouldNotSaveGift_whenTransferFails() {
        doThrow(new RuntimeException("Not enough ArenaTalk coins"))
                .when(walletService).transferCoins(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> giftService.sendGift(request));

        verify(giftRepository, never()).save(any());
    }

    @Test
    void sendGift_shouldNotSendWebSocket_whenTransferFails() {
        doThrow(new RuntimeException("Not enough ArenaTalk coins"))
                .when(walletService).transferCoins(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> giftService.sendGift(request));

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}