package com.codearena.module4_shop.service;

import com.codearena.module4_shop.entity.LoyaltyPoints;
import com.codearena.module4_shop.repository.LoyaltyPointsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyPointsRepository loyaltyPointsRepository;
    private final CouponService couponService;
    private final SimpMessagingTemplate messagingTemplate;

    // ── CONSTANTS ─────────────────────────────────
    private static final int POINTS_PER_DOLLAR  = 1;
    private static final int POINTS_PER_REWARD  = 100;
    private static final double REWARD_VALUE    = 5.0;

    // ── ECO BONUS CONSTANTS ───────────────────────
    // Rewards users who buy sustainable products
    // Linked to UN SDG 12 — Responsible Consumption
    private static final int ECO_BONUS_HIGH     = 50;  // ECO 8-10 → +50 points 🌱
    private static final int ECO_BONUS_MID      = 20;  // ECO 5-7  → +20 points 🌿
    // ECO 1-4 → no bonus ⚠️

    // ── GET POINTS ────────────────────────────────
    public int getPoints(String participantId) {
        return loyaltyPointsRepository
                .findByParticipantId(participantId)
                .map(LoyaltyPoints::getPoints)
                .orElse(0);
    }

    // ── EARN POINTS ───────────────────────────────
    // Standard earning: 1 point per $1 spent
    @Transactional
    public int earnPoints(String participantId, double orderTotal) {
        int earned = (int) (orderTotal * POINTS_PER_DOLLAR);
        LoyaltyPoints lp = loyaltyPointsRepository
                .findByParticipantId(participantId)
                .orElse(LoyaltyPoints.builder()
                        .participantId(participantId)
                        .points(0)
                        .build());

        int pointsBefore = lp.getPoints();
        lp.setPoints(lp.getPoints() + earned);
        loyaltyPointsRepository.save(lp);

        log.info("Participant {} earned {} points. Total: {}",
                participantId, earned, lp.getPoints());

        // ── CHECK MILESTONES ──────────────────────────
        // Check if user crossed a milestone threshold
        checkAndRewardMilestone(participantId, pointsBefore, lp.getPoints());

        return earned;
    }

    // ── EARN ECO BONUS POINTS ─────────────────────
    // Called after checkout when average eco score is calculated
    // Incentivizes users to buy sustainable products
    //
    // ECO 8-10 → +50 bonus points 🌱 (Excellent/Good eco products)
    // ECO 5-7  → +20 bonus points 🌿 (Average eco products)
    // ECO 1-4  → no bonus ⚠️       (Poor eco — no incentive)
    //
    // WHY THIS MATTERS for SDG 12:
    // By rewarding eco-friendly purchases with bonus points,
    // we create a financial incentive for responsible consumption
    // without forcing users — they choose, we reward
    @Transactional
    public int earnEcoBonus(String participantId, int avgEcoScore) {
        int bonus = 0;

        if (avgEcoScore >= 8) {
            bonus = ECO_BONUS_HIGH;  // Excellent eco choices
        } else if (avgEcoScore >= 5) {
            bonus = ECO_BONUS_MID;   // Good eco choices
        }
        // ECO 1-4: no bonus — we don't penalize, just don't reward

        if (bonus == 0) {
            log.info("No eco bonus for participant {} (ECO {}/10)",
                    participantId, avgEcoScore);
            return 0;
        }

        LoyaltyPoints lp = loyaltyPointsRepository
                .findByParticipantId(participantId)
                .orElse(LoyaltyPoints.builder()
                        .participantId(participantId)
                        .points(0)
                        .build());

        lp.setPoints(lp.getPoints() + bonus);
        loyaltyPointsRepository.save(lp);

        int pointsBefore = lp.getPoints() - bonus;
        checkAndRewardMilestone(participantId, pointsBefore, lp.getPoints());

        log.info("🌱 Eco bonus: participant {} earned +{} points for ECO {}/10",
                participantId, bonus, avgEcoScore);
        return bonus;
    }
    // ── DEDUCT POINTS ─────────────────────────────
// Called when order is cancelled
// Removes points that were earned from that order
// Cannot go below 0
    @Transactional
    public void deductPoints(String participantId, int pointsToDeduct) {
        if (pointsToDeduct <= 0) return;

        LoyaltyPoints lp = loyaltyPointsRepository
                .findByParticipantId(participantId)
                .orElse(null);

        if (lp == null) return; // no points to deduct

        int newPoints = Math.max(0, lp.getPoints() - pointsToDeduct);
        lp.setPoints(newPoints);
        loyaltyPointsRepository.save(lp);
        log.info("Deducted {} points from participant {} (cancelled order). New total: {}",
                pointsToDeduct, participantId, newPoints);
    }
    // ── REDEEM POINTS ─────────────────────────────
    @Transactional
    public double redeemPoints(String participantId, int pointsToRedeem) {
        LoyaltyPoints lp = loyaltyPointsRepository
                .findByParticipantId(participantId)
                .orElseThrow(() -> new RuntimeException(
                        "No points found for participant"));

        if (lp.getPoints() < pointsToRedeem) {
            throw new RuntimeException("Insufficient points");
        }

        if (pointsToRedeem % POINTS_PER_REWARD != 0) {
            throw new RuntimeException(
                    "Points must be redeemed in multiples of " + POINTS_PER_REWARD);
        }

        double discount = ((double) pointsToRedeem / POINTS_PER_REWARD) * REWARD_VALUE;        lp.setPoints(lp.getPoints() - pointsToRedeem);
        loyaltyPointsRepository.save(lp);

        log.info("Participant {} redeemed {} points for ${} off",
                participantId, pointsToRedeem, discount);
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
        return ((double) redeemable / POINTS_PER_REWARD) * REWARD_VALUE;    }

    // ── MILESTONE REWARD ──────────────────────────
// Triggered when user crosses 100, 200, or 500 points
// Auto-generates a unique coupon and notifies user via WebSocket
    private void checkAndRewardMilestone(String participantId, int before, int after) {
        int[] milestones = {100, 200, 500};

        for (int milestone : milestones) {
            // User crossed this milestone in this transaction
            if (before < milestone && after >= milestone) {
                String couponCode = couponService.generateMilestoneCoupon(milestone);
                double discount = couponService.getDiscountRate(couponCode) * 100;

                log.info("🎉 Milestone reached! {} points → coupon {} ({} % off) for {}",
                        milestone, couponCode, (int) discount, participantId);

                // ── NOTIFY USER VIA WEBSOCKET ─────────
                java.util.Map<String, Object> milestoneNotif = new java.util.HashMap<>();
                milestoneNotif.put("type",       "MILESTONE_REACHED");
                milestoneNotif.put("milestone",  milestone);
                milestoneNotif.put("couponCode", couponCode);
                milestoneNotif.put("discount",   (int) discount);
                milestoneNotif.put("message",    "🎉 You reached " + milestone + " points! Use code "
                        + couponCode + " for " + (int) discount + "% off!");
                messagingTemplate.convertAndSend("/topic/loyalty/" + participantId, milestoneNotif);
            }
        }
    }
}