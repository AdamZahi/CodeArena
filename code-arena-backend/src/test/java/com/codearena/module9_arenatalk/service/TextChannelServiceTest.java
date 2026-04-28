package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.Hub;
import com.codearena.module9_arenatalk.entity.TextChannel;
import com.codearena.module9_arenatalk.repository.HubRepository;
import com.codearena.module9_arenatalk.repository.TextChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextChannelServiceTest {

    @Mock private TextChannelRepository textChannelRepository;
    @Mock private HubRepository         hubRepository;

    @InjectMocks
    private TextChannelService textChannelService;

    private Hub         hub;
    private TextChannel channel;

    @BeforeEach
    void setUp() {
        hub = new Hub();
        hub.setId(1L);
        hub.setName("Test Hub");

        channel = new TextChannel();
        channel.setId(1L);
        channel.setName("general");
        channel.setTopic("Main discussion");
        channel.setHub(hub);
    }

    // ── createChannel ─────────────────────────────────────────────────────────

    @Test
    void createChannel_shouldSaveChannel_whenHubExists() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(textChannelRepository.save(any())).thenReturn(channel);

        TextChannel result = textChannelService.createChannel(1L, channel);

        assertNotNull(result);
        assertEquals("general", result.getName());
        verify(textChannelRepository, times(1)).save(any());
    }

    @Test
    void createChannel_shouldThrowException_whenHubNotFound() {
        when(hubRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                textChannelService.createChannel(99L, channel)
        );
    }

    @Test
    void createChannel_shouldSetHubOnChannel() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(textChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TextChannel newChannel = new TextChannel();
        newChannel.setName("strategy");

        TextChannel result = textChannelService.createChannel(1L, newChannel);

        assertEquals(hub, result.getHub());
    }

    // ── getChannelsByHub ──────────────────────────────────────────────────────

    @Test
    void getChannelsByHub_shouldReturnChannels() {
        when(textChannelRepository.findByHubId(1L)).thenReturn(List.of(channel));

        List<TextChannel> result = textChannelService.getChannelsByHub(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("general", result.get(0).getName());
    }

    @Test
    void getChannelsByHub_shouldReturnEmptyList_whenNoChannels() {
        when(textChannelRepository.findByHubId(1L)).thenReturn(List.of());

        List<TextChannel> result = textChannelService.getChannelsByHub(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getChannelsByHub_shouldReturnMultipleChannels() {
        TextChannel channel2 = new TextChannel();
        channel2.setId(2L);
        channel2.setName("strategy");

        when(textChannelRepository.findByHubId(1L)).thenReturn(List.of(channel, channel2));

        List<TextChannel> result = textChannelService.getChannelsByHub(1L);

        assertEquals(2, result.size());
    }

    // ── deleteChannel ─────────────────────────────────────────────────────────

    @Test
    void deleteChannel_shouldCallDeleteById() {
        doNothing().when(textChannelRepository).deleteById(1L);

        textChannelService.deleteChannel(1L);

        verify(textChannelRepository, times(1)).deleteById(1L);
    }

    // ── updateChannel ─────────────────────────────────────────────────────────

    @Test
    void updateChannel_shouldUpdateNameAndTopic() {
        TextChannel updated = new TextChannel();
        updated.setName("general-updated");
        updated.setTopic("Updated topic");

        when(textChannelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(textChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TextChannel result = textChannelService.updateChannel(1L, updated);

        assertEquals("general-updated", result.getName());
        assertEquals("Updated topic", result.getTopic());
    }

    @Test
    void updateChannel_shouldThrowException_whenChannelNotFound() {
        TextChannel updated = new TextChannel();
        updated.setName("new-name");
        updated.setTopic("new topic");

        when(textChannelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                textChannelService.updateChannel(99L, updated)
        );
    }

    @Test
    void updateChannel_shouldSaveUpdatedChannel() {
        TextChannel updated = new TextChannel();
        updated.setName("new-name");
        updated.setTopic("new topic");

        when(textChannelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(textChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        textChannelService.updateChannel(1L, updated);

        verify(textChannelRepository, times(1)).save(any());
    }
}