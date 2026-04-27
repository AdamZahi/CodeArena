package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.dto.MessageRequestDTO;
import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/arenatalk")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/channels/{channelId}/messages")
    public Message sendMessage(@PathVariable Long channelId, @Valid @RequestBody MessageRequestDTO request) {
        return messageService.sendMessage(channelId, request);
    }

    @GetMapping("/channels/{channelId}/messages")
    public List<Message> getMessagesByChannel(@PathVariable Long channelId) {
        return messageService.getMessagesByChannel(channelId);
    }

    @GetMapping("/channels/{channelId}/messages/pinned")
    public List<Message> getPinnedMessages(@PathVariable Long channelId) {
        return messageService.getPinnedMessagesByChannel(channelId);
    }

    @DeleteMapping("/messages/{messageId}")
    public void deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
    }

    @PutMapping("/messages/{messageId}")
    public Message updateMessage(@PathVariable Long messageId, @RequestBody Message message) {
        return messageService.updateMessage(messageId, message);
    }

    @PutMapping("/messages/{messageId}/pin")
    public Message pinMessage(@PathVariable Long messageId) {
        return messageService.pinMessage(messageId);
    }

    @PutMapping("/messages/{messageId}/unpin")
    public Message unpinMessage(@PathVariable Long messageId) {
        return messageService.unpinMessage(messageId);
    }
}