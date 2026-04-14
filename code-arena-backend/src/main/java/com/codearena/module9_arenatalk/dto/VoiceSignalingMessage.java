package com.codearena.module9_arenatalk.dto;

import lombok.Data;

@Data
public class VoiceSignalingMessage {
    private String type; // offer, answer, ice-candidate, join, leave
    private String fromUserId;
    private String toUserId;
    private String channelId;
    private String payload; // JSON string of SDP or ICE candidate
    private String userName;
}