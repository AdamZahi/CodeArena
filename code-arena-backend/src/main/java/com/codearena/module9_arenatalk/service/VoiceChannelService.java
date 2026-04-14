package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.Hub;
import com.codearena.module9_arenatalk.entity.VoiceChannel;
import com.codearena.module9_arenatalk.repository.HubRepository;
import com.codearena.module9_arenatalk.repository.VoiceChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoiceChannelService {

    private final VoiceChannelRepository voiceChannelRepository;
    private final HubRepository hubRepository;

    public VoiceChannel create(Long hubId, String name) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hub not found"));

        VoiceChannel vc = VoiceChannel.builder()
                .name(name)
                .maxParticipants(8)
                .hub(hub)
                .build();

        return voiceChannelRepository.save(vc);
    }

    public List<VoiceChannel> getByHub(Long hubId) {
        return voiceChannelRepository.findByHubId(hubId);
    }

    public void delete(Long id) {
        voiceChannelRepository.deleteById(id);
    }
}