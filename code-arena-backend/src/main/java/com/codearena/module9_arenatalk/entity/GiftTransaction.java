package com.codearena.module9_arenatalk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gift_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromUserId;
    private String fromUserName;

    private String toUserId;
    private String toUserName;

    private Long hubId;
    private Long voiceChannelId;

    @Enumerated(EnumType.STRING)
    private GiftType giftType;

    private Integer coins;

    private BigDecimal amountMoney;

    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.currency == null) {
            this.currency = "EUR";
        }

        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PAID;
        }
    }
}