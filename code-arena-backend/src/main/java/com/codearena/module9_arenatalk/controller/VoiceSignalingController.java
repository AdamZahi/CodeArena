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
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class VoiceSignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, List<String>> roomParticipants = new ConcurrentHashMap<>();
    private final Map<String, String> userNames = new ConcurrentHashMap<>();

    private void sendToUser(String userId, VoiceSignalingMessage message) {
        messagingTemplate.convertAndSend("/topic/voice/" + userId, message);
    }

    @MessageMapping("/voice/join")
    public void join(@Payload VoiceSignalingMessage message) {
        String channelId = message.getChannelId();
        String userId = message.getFromUserId();
        String userName = message.getUserName();

        System.out.println("🔍 fromUserId: " + userId + " channel: " + channelId + " userName: " + userName);

        userNames.put(userId, userName);
        roomParticipants.computeIfAbsent(channelId, k -> new ArrayList<>());
        List<String> participants = roomParticipants.get(channelId);

        // ✅ Fix doublon — ne pas rejoindre si déjà dans la room
        if (participants.contains(userId)) {
            System.out.println("⚠️ User " + userId + " already in room, sending current participants only");
            List<String> unique = participants.stream().distinct().collect(Collectors.toList());
            VoiceSignalingMessage rejoined = new VoiceSignalingMessage();
            rejoined.setType("room-participants");
            rejoined.setChannelId(channelId);
            rejoined.setPayload(String.join(",", unique));
            sendToUser(userId, rejoined);
            return;
        }

        // Check room full
        if (participants.size() >= 8) {
            VoiceSignalingMessage full = new VoiceSignalingMessage();
            full.setType("room-full");
            full.setChannelId(channelId);
            sendToUser(userId, full);
            return;
        }

        // Notify existing participants that new user joined
        for (String existingUserId : participants) {
            System.out.println("📢 Notifying " + existingUserId + " that " + userId + " joined");
            VoiceSignalingMessage notify = new VoiceSignalingMessage();
            notify.setType("user-joined");
            notify.setFromUserId(userId);
            notify.setUserName(userName);
            notify.setChannelId(channelId);
            sendToUser(existingUserId, notify);
        }

        // Add new user to room
        participants.add(userId);

        // ✅ Fix doublon — déduplique avant d'envoyer
        List<String> uniqueParticipants = participants.stream().distinct().collect(Collectors.toList());

        System.out.println("📋 Sending room participants to " + userId + ": " + String.join(",", uniqueParticipants));
        VoiceSignalingMessage joined = new VoiceSignalingMessage();
        joined.setType("room-participants");
        joined.setChannelId(channelId);
        joined.setPayload(String.join(",", uniqueParticipants));
        sendToUser(userId, joined);
    }

    @MessageMapping("/voice/leave")
    public void leave(@Payload VoiceSignalingMessage message) {
        String channelId = message.getChannelId();
        String userId = message.getFromUserId();

        System.out.println("👋 User leaving: " + userId + " from channel: " + channelId);

        List<String> participants = roomParticipants.get(channelId);
        if (participants != null) {
            participants.removeIf(id -> id.equals(userId));
            userNames.remove(userId);

            for (String participantId : participants) {
                VoiceSignalingMessage notify = new VoiceSignalingMessage();
                notify.setType("user-left");
                notify.setFromUserId(userId);
                notify.setChannelId(channelId);
                sendToUser(participantId, notify);
            }
        }
    }

    @MessageMapping("/voice/signal")
    public void signal(@Payload VoiceSignalingMessage message) {
        if ("kick".equals(message.getType())) {
            String channelId = message.getChannelId();
            String targetUserId = message.getToUserId();

            List<String> participants = roomParticipants.get(channelId);
            if (participants != null) {
                participants.removeIf(id -> id.equals(targetUserId));

                for (String participantId : participants) {
                    VoiceSignalingMessage notify = new VoiceSignalingMessage();
                    notify.setType("user-left");
                    notify.setFromUserId(targetUserId);
                    notify.setChannelId(channelId);
                    sendToUser(participantId, notify);
                }
            }

            VoiceSignalingMessage kickedMsg = new VoiceSignalingMessage();
            kickedMsg.setType("kicked");
            kickedMsg.setChannelId(channelId);
            kickedMsg.setFromUserId(message.getFromUserId());
            sendToUser(targetUserId, kickedMsg);
            return;
        }

        System.out.println("📡 Forwarding signal: " + message.getType() + " to: " + message.getToUserId());
        sendToUser(message.getToUserId(), message);
    }
}