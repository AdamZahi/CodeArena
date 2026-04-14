package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.VoiceChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoiceChannelRepository extends JpaRepository<VoiceChannel, Long> {
    List<VoiceChannel> findByHubId(Long hubId);
}