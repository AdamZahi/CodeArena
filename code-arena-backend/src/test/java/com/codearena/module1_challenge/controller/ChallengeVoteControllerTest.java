package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.ChallengeVoteResponseDto;
import com.codearena.module1_challenge.service.ChallengeVoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeVoteControllerTest {

    @Mock
    private ChallengeVoteService challengeVoteService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private ChallengeVoteController voteController;

    private final Long CHALLENGE_ID = 1L;
    private final String USER_ID = "auth0|user123";

    @Nested
    @DisplayName("POST /api/challenges/{id}/upvote")
    class Upvote {

        @Test
        @DisplayName("should call upvote and return 200")
        void shouldUpvote() {
            ChallengeVoteResponseDto dto = ChallengeVoteResponseDto.builder()
                    .upvotes(5).downvotes(1).userVote("UPVOTE").build();

            when(jwt.getSubject()).thenReturn(USER_ID);
            when(challengeVoteService.upvote(CHALLENGE_ID, USER_ID)).thenReturn(dto);

            ResponseEntity<ChallengeVoteResponseDto> response =
                    voteController.upvote(CHALLENGE_ID, jwt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getUpvotes()).isEqualTo(5);
            assertThat(response.getBody().getUserVote()).isEqualTo("UPVOTE");
        }
    }

    @Nested
    @DisplayName("POST /api/challenges/{id}/downvote")
    class Downvote {

        @Test
        @DisplayName("should call downvote and return 200")
        void shouldDownvote() {
            ChallengeVoteResponseDto dto = ChallengeVoteResponseDto.builder()
                    .upvotes(3).downvotes(2).userVote("DOWNVOTE").build();

            when(jwt.getSubject()).thenReturn(USER_ID);
            when(challengeVoteService.downvote(CHALLENGE_ID, USER_ID)).thenReturn(dto);

            ResponseEntity<ChallengeVoteResponseDto> response =
                    voteController.downvote(CHALLENGE_ID, jwt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getDownvotes()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("GET /api/challenges/{id}/votes")
    class GetVotes {

        @Test
        @DisplayName("should return vote summary for the challenge")
        void shouldReturnVotes() {
            ChallengeVoteResponseDto dto = ChallengeVoteResponseDto.builder()
                    .upvotes(10).downvotes(3).userVote(null).build();

            when(jwt.getSubject()).thenReturn(USER_ID);
            when(challengeVoteService.getVotes(CHALLENGE_ID, USER_ID)).thenReturn(dto);

            ResponseEntity<ChallengeVoteResponseDto> response =
                    voteController.getVotes(CHALLENGE_ID, jwt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getUpvotes()).isEqualTo(10);
            assertThat(response.getBody().getUserVote()).isNull();
        }
    }
}
