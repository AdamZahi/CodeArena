package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.TextChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TextChannelRepository extends JpaRepository<TextChannel, Long> {
    List<TextChannel> findByHubId(Long hubId);
}