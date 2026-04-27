package com.codearena.module9_arenatalk.dto;

import com.codearena.module9_arenatalk.entity.GiftType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GiftEventDTO {

    private Long giftId;

    private String fromUserId;
    private String fromUserName;

    private String toUserId;
    private String toUserName;

    private Long hubId;
    private Long voiceChannelId;

    private GiftType giftType;

    private Integer coins;

    private String message;
}