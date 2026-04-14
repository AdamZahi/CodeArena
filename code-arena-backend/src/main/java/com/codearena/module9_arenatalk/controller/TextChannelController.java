package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.service.TextChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/arenatalk")
@RequiredArgsConstructor
public class TextChannelController {

    private final TextChannelService textChannelService;

    @PostMapping("/hubs/{hubId}/channels")
    public TextChannel createChannel(@PathVariable Long hubId, @RequestBody TextChannel channel) {
        return textChannelService.createChannel(hubId, channel);
    }

    @GetMapping("/hubs/{hubId}/channels")
    public List<TextChannel> getChannelsByHub(@PathVariable Long hubId) {
        return textChannelService.getChannelsByHub(hubId);
    }

    @DeleteMapping("/channels/{channelId}")
    public void deleteChannel(@PathVariable Long channelId) {
        textChannelService.deleteChannel(channelId);
    }
    @PutMapping("/channels/{channelId}")
    public TextChannel updateChannel(@PathVariable Long channelId, @RequestBody TextChannel channel) {
        return textChannelService.updateChannel(channelId, channel);
    }
}