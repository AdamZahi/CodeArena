package com.codearena.module4_shop.service;

import com.codearena.module4_shop.entity.LoyaltyPoints;
import com.codearena.module4_shop.repository.LoyaltyPointsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyPointsRepository loyaltyPointsRepository;

    // ── CONSTANTS ─────────────────────────────────
    // 1 point per $1 spent
    private static final int POINTS_PER_DOLLAR = 1;
    // 100 points = $5 off
    private static final int POINTS_PER_REWARD = 100;
    private static final double REWARD_VALUE = 5.0;

    // ── GET POINTS ────────────────────────────────
    public int getPoints(String participantId) {
        return loyaltyPointsRepository
                .findByParticipantId(participantId)
                .map(LoyaltyPoints::getPoints)
                .orElse(0);
    }

    // ── EARN POINTS ───────────────────────────────
    @Transactional
    public int earnPoints(String participantId, double orderTotal) {
        int earned = (int) orderTotal * POINTS_PER_DOLLAR;

        LoyaltyPoints lp = loyaltyPointsRepository
                .findByParticipantId(participantId)
                .orElse(LoyaltyPoints.builder()
                        .participantId(participantId)
                        .points(0)
                        .build());

        lp.setPoints(lp.getPoints() + earned);
        loyaltyPointsRepository.save(lp);
        log.info("Participant {} earned {} points. Total: {}", participantId, earned, lp.getPoints());
        return earned;
    }

    // ── REDEEM POINTS ─────────────────────────────
    @Transactional
    public double redeemPoints(String participantId, int pointsToRedeem) {
        LoyaltyPoints lp = loyaltyPointsRepository
                .findByParticipantId(participantId)
                .orElseThrow(() -> new RuntimeException("No points found for participant"));

        if (lp.getPoints() < pointsToRedeem) {
            throw new RuntimeException("Insufficient points");
        }

        if (pointsToRedeem % POINTS_PER_REWARD != 0) {
            throw new RuntimeException("Points must be redeemed in multiples of " + POINTS_PER_REWARD);
        }

        double discount = (pointsToRedeem / POINTS_PER_REWARD) * REWARD_VALUE;
        lp.setPoints(lp.getPoints() - pointsToRedeem);
        loyaltyPointsRepository.save(lp);

        log.info("Participant {} redeemed {} points for ${} off", participantId, pointsToRedeem, discount);
        return discount;
    }

    // ── CAN REDEEM ────────────────────────────────
    public boolean canRedeem(String participantId) {
        return getPoints(participantId) >= POINTS_PER_REWARD;
    }

    // ── GET REWARD VALUE ──────────────────────────
    public double getRedeemableValue(String participantId) {
        int points = getPoints(participantId);
        int redeemable = (points / POINTS_PER_REWARD) * POINTS_PER_REWARD;
        return (redeemable / POINTS_PER_REWARD) * REWARD_VALUE;
    }
}