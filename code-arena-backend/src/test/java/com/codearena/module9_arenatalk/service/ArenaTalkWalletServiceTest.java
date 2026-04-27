package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.ArenaTalkWallet;
import com.codearena.module9_arenatalk.repository.ArenaTalkWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArenaTalkWalletServiceTest {

    @Mock
    private ArenaTalkWalletRepository walletRepository;

    @InjectMocks
    private ArenaTalkWalletService walletService;

    private ArenaTalkWallet wallet;

    @BeforeEach
    void setUp() {
        wallet = ArenaTalkWallet.builder()
                .userId("user1")
                .userName("Test User")
                .balance(100)
                .build();
    }

    // ── getOrCreateWallet ─────────────────────────────────────────────────────

    @Test
    void getOrCreateWallet_shouldReturnExistingWallet() {
        when(walletRepository.findByUserId("user1"))
                .thenReturn(Optional.of(wallet));

        ArenaTalkWallet result = walletService.getOrCreateWallet("user1", "Test User");

        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        assertEquals(100, result.getBalance());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getOrCreateWallet_shouldCreateNewWallet_withDefaultBalance() {
        when(walletRepository.findByUserId("newUser"))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(ArenaTalkWallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ArenaTalkWallet result = walletService.getOrCreateWallet("newUser", "New User");

        assertNotNull(result);
        assertEquals(100, result.getBalance());
        assertEquals("newUser", result.getUserId());
        assertEquals("New User", result.getUserName());
        verify(walletRepository, times(1)).save(any());
    }

    // ── addCoins ──────────────────────────────────────────────────────────────

    @Test
    void addCoins_shouldIncreaseBalance() {
        when(walletRepository.findByUserId("user1"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArenaTalkWallet result = walletService.addCoins("user1", "Test User", 500);

        assertEquals(600, result.getBalance());
    }

    @Test
    void addCoins_shouldSaveWallet() {
        when(walletRepository.findByUserId("user1"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.addCoins("user1", "Test User", 200);

        verify(walletRepository, times(1)).save(any());
    }

    @Test
    void addCoins_shouldReturnUpdatedWallet() {
        when(walletRepository.findByUserId("user1"))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArenaTalkWallet result = walletService.addCoins("user1", "Test User", 1000);

        assertNotNull(result);
        assertEquals(1100, result.getBalance());
    }

    @Test
    void addCoins_shouldCreateWalletIfNotExists() {
        when(walletRepository.findByUserId("newUser"))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArenaTalkWallet result = walletService.addCoins("newUser", "New User", 200);

        // default 100 + 200 = 300
        assertEquals(300, result.getBalance());
    }

    // ── transferCoins ─────────────────────────────────────────────────────────

    @Test
    void transferCoins_shouldDeductFromSender_andAddToReceiver() {
        ArenaTalkWallet sender   = ArenaTalkWallet.builder().userId("s1").userName("Sender").balance(200).build();
        ArenaTalkWallet receiver = ArenaTalkWallet.builder().userId("r1").userName("Receiver").balance(50).build();

        when(walletRepository.findByUserId("s1")).thenReturn(Optional.of(sender));
        when(walletRepository.findByUserId("r1")).thenReturn(Optional.of(receiver));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.transferCoins("s1", "Sender", "r1", "Receiver", 50);

        assertEquals(150, sender.getBalance());
        assertEquals(100, receiver.getBalance());
    }

    @Test
    void transferCoins_shouldThrowException_whenInsufficientBalance() {
        ArenaTalkWallet sender   = ArenaTalkWallet.builder().userId("s1").userName("Sender").balance(10).build();
        ArenaTalkWallet receiver = ArenaTalkWallet.builder().userId("r1").userName("Receiver").balance(50).build();

        when(walletRepository.findByUserId("s1")).thenReturn(Optional.of(sender));
        when(walletRepository.findByUserId("r1")).thenReturn(Optional.of(receiver));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                walletService.transferCoins("s1", "Sender", "r1", "Receiver", 50)
        );

        assertEquals("Not enough ArenaTalk coins", ex.getMessage());
    }

    @Test
    void transferCoins_shouldSaveBothWallets() {
        ArenaTalkWallet sender   = ArenaTalkWallet.builder().userId("s1").userName("Sender").balance(200).build();
        ArenaTalkWallet receiver = ArenaTalkWallet.builder().userId("r1").userName("Receiver").balance(50).build();

        when(walletRepository.findByUserId("s1")).thenReturn(Optional.of(sender));
        when(walletRepository.findByUserId("r1")).thenReturn(Optional.of(receiver));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.transferCoins("s1", "Sender", "r1", "Receiver", 25);

        verify(walletRepository, times(2)).save(any());
    }

    @Test
    void transferCoins_shouldNotSave_whenInsufficientBalance() {
        ArenaTalkWallet sender   = ArenaTalkWallet.builder().userId("s1").userName("Sender").balance(5).build();
        ArenaTalkWallet receiver = ArenaTalkWallet.builder().userId("r1").userName("Receiver").balance(50).build();

        when(walletRepository.findByUserId("s1")).thenReturn(Optional.of(sender));
        when(walletRepository.findByUserId("r1")).thenReturn(Optional.of(receiver));

        assertThrows(RuntimeException.class, () ->
                walletService.transferCoins("s1", "Sender", "r1", "Receiver", 25)
        );

        verify(walletRepository, never()).save(any());
    }

    @Test
    void transferCoins_shouldAllowExactBalance() {
        ArenaTalkWallet sender   = ArenaTalkWallet.builder().userId("s1").userName("Sender").balance(50).build();
        ArenaTalkWallet receiver = ArenaTalkWallet.builder().userId("r1").userName("Receiver").balance(0).build();

        when(walletRepository.findByUserId("s1")).thenReturn(Optional.of(sender));
        when(walletRepository.findByUserId("r1")).thenReturn(Optional.of(receiver));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletService.transferCoins("s1", "Sender", "r1", "Receiver", 50);

        assertEquals(0,  sender.getBalance());
        assertEquals(50, receiver.getBalance());
    }
}