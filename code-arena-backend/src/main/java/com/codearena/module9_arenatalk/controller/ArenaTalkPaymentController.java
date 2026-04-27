package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.service.ArenaTalkWalletService;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/arenatalk/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ArenaTalkPaymentController {

    private final ArenaTalkWalletService walletService;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody Map<String, Object> request) {
        try {
            String userId = String.valueOf(request.get("userId"));
            String userName = String.valueOf(request.get("userName"));
            Integer coins = Integer.parseInt(String.valueOf(request.get("coins")));

            if (userId == null || userId.isBlank() || userName == null || userName.isBlank() || coins <= 0) {
                return ResponseEntity.badRequest().body("Missing or invalid data");
            }

            Stripe.apiKey = stripeSecretKey;

            long amountInCents = coins;
            // Example: 500 coins = 5.00 EUR because 500 cents = 5 EUR

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:4200/arenatalk/workspace?payment=success&coins=" + coins)
                    .setCancelUrl("http://localhost:4200/arenatalk/workspace?payment=cancel")
                    .putMetadata("userId", userId)
                    .putMetadata("userName", userName)
                    .putMetadata("coins", String.valueOf(coins))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(coins + " ArenaTalk Coins")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

                if (session != null) {
                    String userId = session.getMetadata().get("userId");
                    String userName = session.getMetadata().get("userName");
                    Integer coins = Integer.parseInt(session.getMetadata().get("coins"));

                    walletService.addCoins(userId, userName, coins);
                }
            }

            return ResponseEntity.ok("success");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("webhook error");
        }
    }
}