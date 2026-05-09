package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.dto.ReadReceiptDTO;
import com.codearena.module9_arenatalk.service.MessageReadReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arenatalk")
@RequiredArgsConstructor
public class MessageReadReceiptController {

    private final MessageReadReceiptService readReceiptService;

    @PostMapping("/channels/{channelId}/read")
    public void markChannelAsRead(@PathVariable Long channelId,
                                  @RequestParam String keycloakId) {
        readReceiptService.markChannelMessagesAsRead(channelId, keycloakId);
    }

    @GetMapping("/messages/{messageId}/read-status")
    public ReadReceiptDTO getReadStatus(@PathVariable Long messageId,
                                        @RequestParam String keycloakId) {
        return readReceiptService.getReadStatus(messageId, keycloakId);
    }
}