package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.entity.VoiceChannel;
import com.codearena.module9_arenatalk.service.VoiceChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/arenatalk/hubs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class VoiceChannelController {

    private final VoiceChannelService voiceChannelService;

    @PostMapping("/{hubId}/voice-channels")
    public ResponseEntity<VoiceChannel> create(@PathVariable Long hubId,
                                               @RequestParam String name) {
        return ResponseEntity.ok(voiceChannelService.create(hubId, name));
    }

    @GetMapping("/{hubId}/voice-channels")
    public ResponseEntity<List<VoiceChannel>> getByHub(@PathVariable Long hubId) {
        return ResponseEntity.ok(voiceChannelService.getByHub(hubId));
    }

    @DeleteMapping("/voice-channels/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        voiceChannelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}