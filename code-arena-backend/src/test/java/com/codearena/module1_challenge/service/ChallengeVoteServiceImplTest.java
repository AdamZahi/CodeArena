package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeVoteResponseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.ChallengeVote;
import com.codearena.module1_challenge.entity.VoteType;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.ChallengeVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeVoteServiceImplTest {

    @Mock
    private ChallengeVoteRepository voteRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private ChallengeVoteServiceImpl voteService;

    private Challenge challenge;
    private final Long CHALLENGE_ID = 1L;
    private final String USER_ID = "auth0|user123";

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder().id(CHALLENGE_ID).title("Two Sum").build();
    }

    @Nested
    @DisplayName("upvote")
    class Upvote {

        @Test
        @DisplayName("should create a new UPVOTE when user has no previous vote")
        void shouldCreateNewUpvote() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(voteRepository.findByChallengeIdAndUserId(CHALLENGE_ID, USER_ID))
                    .thenReturn(Optional.empty())           // toggleVote lookup
                    .thenReturn(Optional.empty());          // getVotes lookup
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.UPVOTE)).thenReturn(1L);
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.DOWNVOTE)).thenReturn(0L);

            ChallengeVoteResponseDto result = voteService.upvote(CHALLENGE_ID, USER_ID);

            ArgumentCaptor<ChallengeVote> captor = ArgumentCaptor.forClass(ChallengeVote.class);
            verify(voteRepository).save(captor.capture());
            assertThat(captor.getValue().getVoteType()).isEqualTo(VoteType.UPVOTE);
            assertThat(result.getUpvotes()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should remove vote when user clicks UPVOTE again (toggle off)")
        void shouldRemoveUpvoteOnSecondClick() {
            ChallengeVote existing = ChallengeVote.builder()
                    .id(10L).challenge(challenge).userId(USER_ID)
                    .voteType(VoteType.UPVOTE).build();

            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(voteRepository.findByChallengeIdAndUserId(CHALLENGE_ID, USER_ID))
                    .thenReturn(Optional.of(existing))     // toggleVote lookup
                    .thenReturn(Optional.empty());         // getVotes lookup after delete
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.UPVOTE)).thenReturn(0L);
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.DOWNVOTE)).thenReturn(0L);

            ChallengeVoteResponseDto result = voteService.upvote(CHALLENGE_ID, USER_ID);

            verify(voteRepository).delete(existing);
            assertThat(result.getUpvotes()).isEqualTo(0L);
            assertThat(result.getUserVote()).isNull();
        }

        @Test
        @DisplayName("should switch from DOWNVOTE to UPVOTE")
        void shouldSwitchFromDownvoteToUpvote() {
            ChallengeVote existing = ChallengeVote.builder()
                    .id(10L).challenge(challenge).userId(USER_ID)
                    .voteType(VoteType.DOWNVOTE).build();

            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(voteRepository.findByChallengeIdAndUserId(CHALLENGE_ID, USER_ID))
                    .thenReturn(Optional.of(existing))     // toggleVote: existing is DOWNVOTE
                    .thenReturn(Optional.of(existing));    // getVotes: now UPVOTE (mutated)
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.UPVOTE)).thenReturn(1L);
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.DOWNVOTE)).thenReturn(0L);

            ChallengeVoteResponseDto result = voteService.upvote(CHALLENGE_ID, USER_ID);

            verify(voteRepository).save(existing);
            assertThat(existing.getVoteType()).isEqualTo(VoteType.UPVOTE);
            assertThat(result.getUpvotes()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("downvote")
    class Downvote {

        @Test
        @DisplayName("should create a new DOWNVOTE when user has no previous vote")
        void shouldCreateNewDownvote() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(voteRepository.findByChallengeIdAndUserId(CHALLENGE_ID, USER_ID))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.empty());
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.UPVOTE)).thenReturn(0L);
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.DOWNVOTE)).thenReturn(1L);

            ChallengeVoteResponseDto result = voteService.downvote(CHALLENGE_ID, USER_ID);

            ArgumentCaptor<ChallengeVote> captor = ArgumentCaptor.forClass(ChallengeVote.class);
            verify(voteRepository).save(captor.capture());
            assertThat(captor.getValue().getVoteType()).isEqualTo(VoteType.DOWNVOTE);
            assertThat(result.getDownvotes()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getVotes")
    class GetVotes {

        @Test
        @DisplayName("should return correct vote counts and user's current vote")
        void shouldReturnVoteSummary() {
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.UPVOTE)).thenReturn(5L);
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.DOWNVOTE)).thenReturn(2L);

            ChallengeVote userVote = ChallengeVote.builder().voteType(VoteType.UPVOTE).build();
            when(voteRepository.findByChallengeIdAndUserId(CHALLENGE_ID, USER_ID))
                    .thenReturn(Optional.of(userVote));

            ChallengeVoteResponseDto result = voteService.getVotes(CHALLENGE_ID, USER_ID);

            assertThat(result.getUpvotes()).isEqualTo(5L);
            assertThat(result.getDownvotes()).isEqualTo(2L);
            assertThat(result.getUserVote()).isEqualTo("UPVOTE");
        }

        @Test
        @DisplayName("should return null userVote when user has not voted")
        void shouldReturnNullUserVote() {
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.UPVOTE)).thenReturn(0L);
            when(voteRepository.countByChallengeIdAndVoteType(CHALLENGE_ID, VoteType.DOWNVOTE)).thenReturn(0L);
            when(voteRepository.findByChallengeIdAndUserId(CHALLENGE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            ChallengeVoteResponseDto result = voteService.getVotes(CHALLENGE_ID, USER_ID);

            assertThat(result.getUserVote()).isNull();
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw RuntimeException when challenge not found during vote")
        void shouldThrowWhenChallengeNotFound() {
            when(challengeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voteService.upvote(999L, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Challenge not found: 999");
        }
    }
}
