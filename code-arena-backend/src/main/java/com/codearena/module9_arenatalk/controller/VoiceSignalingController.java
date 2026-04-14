package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.dto.VoiceSignalingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class VoiceSignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    // channelId -> list of userIds in room
    private final Map<String, List<String>> roomParticipants = new ConcurrentHashMap<>();

    @MessageMapping("/voice/join")
    public void join(@Payload VoiceSignalingMessage message) {
        String channelId = message.getChannelId();
        String userId = message.getFromUserId();

        roomParticipants.computeIfAbsent(channelId, k -> new ArrayList<>());
        List<String> participants = roomParticipants.get(channelId);

        if (participants.size() >= 8) {
            // Room full - notify user
            VoiceSignalingMessage full = new VoiceSignalingMessage();
            full.setType("room-full");
            full.setChannelId(channelId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/voice", full);
            return;
        }

        // Tell the new user about existing participants
        for (String existingUserId : participants) {
            VoiceSignalingMessage notify = new VoiceSignalingMessage();
            notify.setType("user-joined");
            notify.setFromUserId(userId);
            notify.setUserName(message.getUserName());
            notify.setChannelId(channelId);
            messagingTemplate.convertAndSendToUser(existingUserId, "/queue/voice", notify);
        }

        participants.add(userId);

        // Send current participants list to new user
        VoiceSignalingMessage joined = new VoiceSignalingMessage();
        joined.setType("room-participants");
        joined.setChannelId(channelId);
        joined.setPayload(String.join(",", participants));
        messagingTemplate.convertAndSendToUser(userId, "/queue/voice", joined);
    }

    @MessageMapping("/voice/leave")
    public void leave(@Payload VoiceSignalingMessage message) {
        String channelId = message.getChannelId();
        String userId = message.getFromUserId();

        List<String> participants = roomParticipants.get(channelId);
        if (participants != null) {
            participants.remove(userId);

            // Notify others
            for (String participantId : participants) {
                VoiceSignalingMessage notify = new VoiceSignalingMessage();
                notify.setType("user-left");
                notify.setFromUserId(userId);
                notify.setChannelId(channelId);
                messagingTemplate.convertAndSendToUser(participantId, "/queue/voice", notify);
            }
        }
    }

    @MessageMapping("/voice/signal")
    public void signal(@Payload VoiceSignalingMessage message) {
        // Forward offer/answer/ice-candidate to target user
        messagingTemplate.convertAndSendToUser(
                message.getToUserId(),
                "/queue/voice",
                message
        );
    }
}