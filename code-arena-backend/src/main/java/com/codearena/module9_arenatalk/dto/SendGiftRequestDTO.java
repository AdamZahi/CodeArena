package com.codearena.module9_arenatalk.dto;

import com.codearena.module9_arenatalk.entity.GiftType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SendGiftRequestDTO {

    private String fromUserId;
    private String fromUserName;

    private String toUserId;
    private String toUserName;

    private Long hubId;
    private Long voiceChannelId;

    private GiftType giftType;

    private Integer coins;

    private BigDecimal amountMoney;

    private String currency;
}