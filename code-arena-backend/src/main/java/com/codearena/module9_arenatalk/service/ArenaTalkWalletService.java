package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.ArenaTalkWallet;
import com.codearena.module9_arenatalk.repository.ArenaTalkWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArenaTalkWalletService {

    private final ArenaTalkWalletRepository walletRepository;

    public ArenaTalkWallet getOrCreateWallet(String userId, String userName) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(
                        ArenaTalkWallet.builder()
                                .userId(userId)
                                .userName(userName)
                                .balance(100)
                                .build()
                ));
    }

    public void transferCoins(String fromUserId, String fromUserName,
                              String toUserId, String toUserName,
                              Integer coins) {

        ArenaTalkWallet senderWallet = getOrCreateWallet(fromUserId, fromUserName);
        ArenaTalkWallet receiverWallet = getOrCreateWallet(toUserId, toUserName);

        if (senderWallet.getBalance() < coins) {
            throw new RuntimeException("Not enough ArenaTalk coins");
        }

        senderWallet.setBalance(senderWallet.getBalance() - coins);
        receiverWallet.setBalance(receiverWallet.getBalance() + coins);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);
    }
    public ArenaTalkWallet addCoins(String userId, String userName, Integer coins) {
        ArenaTalkWallet wallet = getOrCreateWallet(userId, userName);
        wallet.setBalance(wallet.getBalance() + coins);
        return walletRepository.save(wallet);
    }
}