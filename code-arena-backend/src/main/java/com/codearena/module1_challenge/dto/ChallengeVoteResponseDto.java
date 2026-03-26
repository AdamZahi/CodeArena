package com.codearena.module1_challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeVoteResponseDto {
    private long upvotes;
    private long downvotes;
    private String userVote; // "UPVOTE", "DOWNVOTE", or null
}
