package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleFeedResponse {
    private List<BattleFeedItemResponse> live;
    private List<BattleFeedItemResponse> open;
    private List<BattleFeedItemResponse> recent;
    private long totalLiveCount;
    private long totalOpenCount;
}
