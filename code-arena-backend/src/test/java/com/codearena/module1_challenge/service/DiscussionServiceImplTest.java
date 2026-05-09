package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.CommentRequestDto;
import com.codearena.module1_challenge.dto.CommentResponseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.ChallengeComment;
import com.codearena.module1_challenge.repository.ChallengeCommentRepository;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceImplTest {

    @Mock
    private ChallengeCommentRepository commentRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private DiscussionServiceImpl discussionService;

    private Challenge challenge;
    private final Long CHALLENGE_ID = 1L;
    private final String USER_ID = "auth0|user123";
    private final String USER_NAME = "JohnDoe";

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder().id(CHALLENGE_ID).title("Two Sum").build();
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("should create a comment and return the mapped DTO")
        void shouldCreateComment() {
            CommentRequestDto request = CommentRequestDto.builder()
                    .content("Great problem!")
                    .build();

            ChallengeComment saved = ChallengeComment.builder()
                    .id(10L)
                    .challenge(challenge)
                    .userId(USER_ID)
                    .userName(USER_NAME)
                    .content("Great problem!")
                    .createdAt(Instant.now())
                    .build();

            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(commentRepository.save(any(ChallengeComment.class))).thenReturn(saved);

            CommentResponseDto result = discussionService.createComment(CHALLENGE_ID, request, USER_ID, USER_NAME);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getContent()).isEqualTo("Great problem!");
            assertThat(result.getUserName()).isEqualTo(USER_NAME);
            assertThat(result.getChallengeId()).isEqualTo(CHALLENGE_ID);

            ArgumentCaptor<ChallengeComment> captor = ArgumentCaptor.forClass(ChallengeComment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getContent()).isEqualTo("Great problem!");
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw when challenge does not exist")
        void shouldThrowWhenChallengeNotFound() {
            when(challengeRepository.findById(999L)).thenReturn(Optional.empty());

            CommentRequestDto request = CommentRequestDto.builder().content("test").build();

            assertThatThrownBy(() -> discussionService.createComment(999L, request, USER_ID, USER_NAME))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Challenge not found: 999");
        }
    }

    @Nested
    @DisplayName("getCommentsByChallenge")
    class GetComments {

        @Test
        @DisplayName("should return all comments for a challenge ordered by latest first")
        void shouldReturnComments() {
            ChallengeComment comment1 = ChallengeComment.builder()
                    .id(1L).challenge(challenge).userId(USER_ID).userName(USER_NAME)
                    .content("First!").createdAt(Instant.now()).build();

            ChallengeComment comment2 = ChallengeComment.builder()
                    .id(2L).challenge(challenge).userId("user2").userName("Jane")
                    .content("Second!").createdAt(Instant.now()).build();

            when(commentRepository.findByChallengeIdOrderByCreatedAtDesc(CHALLENGE_ID))
                    .thenReturn(List.of(comment2, comment1));

            List<CommentResponseDto> result = discussionService.getCommentsByChallenge(CHALLENGE_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getContent()).isEqualTo("Second!");
            assertThat(result.get(1).getContent()).isEqualTo("First!");
        }

        @Test
        @DisplayName("should return empty list when challenge has no comments")
        void shouldReturnEmptyList() {
            when(commentRepository.findByChallengeIdOrderByCreatedAtDesc(CHALLENGE_ID))
                    .thenReturn(Collections.emptyList());

            List<CommentResponseDto> result = discussionService.getCommentsByChallenge(CHALLENGE_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("should delete comment when user is the author")
        void shouldDeleteOwnComment() {
            ChallengeComment comment = ChallengeComment.builder()
                    .id(10L).challenge(challenge).userId(USER_ID)
                    .content("My comment").build();

            when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

            discussionService.deleteComment(10L, USER_ID);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("should throw when trying to delete another user's comment")
        void shouldThrowWhenDeletingOtherUsersComment() {
            ChallengeComment comment = ChallengeComment.builder()
                    .id(10L).challenge(challenge).userId("other-user")
                    .content("Not yours").build();

            when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> discussionService.deleteComment(10L, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unauthorized");
        }

        @Test
        @DisplayName("should throw when comment does not exist")
        void shouldThrowWhenCommentNotFound() {
            when(commentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discussionService.deleteComment(999L, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Comment not found: 999");
        }
    }
}
