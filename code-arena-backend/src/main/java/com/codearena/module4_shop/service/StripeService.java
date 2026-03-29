package com.codearena.module4_shop.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeService {

    @Value("${app.shop.stripe.secret-key}")
    private String secretKey;

    @Value("${app.shop.stripe.publishable-key}")
    private String publishableKey;

    // ── INITIALIZE STRIPE ─────────────────────────
    // Called once when Spring starts — sets the global Stripe API key
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe initialized successfully");
    }

    // ── CREATE PAYMENT INTENT ─────────────────────
    // Creates a payment intent on Stripe's servers
    // Returns a client_secret that Angular uses to confirm payment
    // amount is in cents (e.g. $29.99 = 2999)
    public Map<String, String> createPaymentIntent(double amount, String currency) throws StripeException {
        // Convert dollars to cents (Stripe uses smallest currency unit)
        long amountInCents = Math.round(amount * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Map<String, String> result = new HashMap<>();
        result.put("clientSecret", intent.getClientSecret());
        result.put("publishableKey", publishableKey);
        result.put("paymentIntentId", intent.getId());

        log.info("Payment intent created: {} for amount: ${}", intent.getId(), amount);
        return result;
    }

    // ── GET PUBLISHABLE KEY ───────────────────────
    // Frontend needs this to initialize Stripe.js
    public String getPublishableKey() {
        return publishableKey;
    }
}