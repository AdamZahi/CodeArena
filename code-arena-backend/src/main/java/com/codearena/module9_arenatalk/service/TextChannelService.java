package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.Hub;
import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.repository.HubRepository;
import com.codearena.module9_arenatalk.repository.TextChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TextChannelService {

    private final TextChannelRepository textChannelRepository;
    private final HubRepository hubRepository;

    public TextChannel createChannel(Long hubId, TextChannel channel) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new RuntimeException("Hub not found with id: " + hubId));

        channel.setHub(hub);
        return textChannelRepository.save(channel);
    }

    public List<TextChannel> getChannelsByHub(Long hubId) {
        return textChannelRepository.findByHubId(hubId);
    }

    public void deleteChannel(Long channelId) {
        textChannelRepository.deleteById(channelId);
    }
    public TextChannel updateChannel(Long channelId, TextChannel updatedChannel) {
        TextChannel existingChannel = textChannelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found with id: " + channelId));

        existingChannel.setName(updatedChannel.getName());
        existingChannel.setTopic(updatedChannel.getTopic());

        return textChannelRepository.save(existingChannel);
    }
}