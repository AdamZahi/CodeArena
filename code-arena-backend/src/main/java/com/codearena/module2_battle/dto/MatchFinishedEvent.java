package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchFinishedEvent {
    private String roomId;
    private String triggerReason;
    private LocalDateTime finishedAt;
    private List<ArenaParticipantProgressResponse> finalStandings;
}
