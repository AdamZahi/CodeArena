package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.Hub;
import com.codearena.module9_arenatalk.entity.VoiceChannel;
import com.codearena.module9_arenatalk.repository.HubRepository;
import com.codearena.module9_arenatalk.repository.VoiceChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceChannelServiceTest {

    @Mock private VoiceChannelRepository voiceChannelRepository;
    @Mock private HubRepository          hubRepository;

    @InjectMocks
    private VoiceChannelService voiceChannelService;

    private Hub          hub;
    private VoiceChannel voiceChannel;

    @BeforeEach
    void setUp() {
        hub = new Hub();
        hub.setId(1L);
        hub.setName("Test Hub");

        voiceChannel = new VoiceChannel();
        voiceChannel.setId(1L);
        voiceChannel.setName("general-voice");
        voiceChannel.setMaxParticipants(8);
        voiceChannel.setHub(hub);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldSaveVoiceChannel_whenHubExists() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(voiceChannelRepository.save(any(VoiceChannel.class))).thenReturn(voiceChannel);

        VoiceChannel result = voiceChannelService.create(1L, "general-voice");

        assertNotNull(result);
        assertEquals("general-voice", result.getName());
        verify(voiceChannelRepository, times(1)).save(any());
    }

    @Test
    void create_shouldThrowException_whenHubNotFound() {
        when(hubRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                voiceChannelService.create(99L, "voice")
        );
    }

    @Test
    void create_shouldSetMaxParticipantsTo8() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(voiceChannelRepository.save(any(VoiceChannel.class))).thenAnswer(inv -> inv.getArgument(0));

        VoiceChannel result = voiceChannelService.create(1L, "voice-room");

        assertEquals(8, result.getMaxParticipants());
    }

    @Test
    void create_shouldSetCorrectHub() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(voiceChannelRepository.save(any(VoiceChannel.class))).thenAnswer(inv -> inv.getArgument(0));

        VoiceChannel result = voiceChannelService.create(1L, "voice-room");

        assertEquals(hub, result.getHub());
    }

    @Test
    void create_shouldSetCorrectName() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(voiceChannelRepository.save(any(VoiceChannel.class))).thenAnswer(inv -> inv.getArgument(0));

        VoiceChannel result = voiceChannelService.create(1L, "my-voice-channel");

        assertEquals("my-voice-channel", result.getName());
    }

    // ── getByHub ──────────────────────────────────────────────────────────────

    @Test
    void getByHub_shouldReturnVoiceChannels() {
        when(voiceChannelRepository.findByHubId(1L)).thenReturn(List.of(voiceChannel));

        List<VoiceChannel> result = voiceChannelService.getByHub(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("general-voice", result.get(0).getName());
    }

    @Test
    void getByHub_shouldReturnEmptyList_whenNoChannels() {
        when(voiceChannelRepository.findByHubId(1L)).thenReturn(List.of());

        List<VoiceChannel> result = voiceChannelService.getByHub(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getByHub_shouldReturnMultipleChannels() {
        VoiceChannel vc2 = new VoiceChannel();
        vc2.setId(2L);
        vc2.setName("gaming-voice");

        when(voiceChannelRepository.findByHubId(1L)).thenReturn(List.of(voiceChannel, vc2));

        List<VoiceChannel> result = voiceChannelService.getByHub(1L);

        assertEquals(2, result.size());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldCallDeleteById() {
        doNothing().when(voiceChannelRepository).deleteById(1L);

        voiceChannelService.delete(1L);

        verify(voiceChannelRepository, times(1)).deleteById(1L);
    }
}