package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.CommentRequestDto;
import com.codearena.module1_challenge.dto.CommentResponseDto;
import com.codearena.module1_challenge.service.DiscussionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionControllerTest {

    @Mock
    private DiscussionService discussionService;

    @Mock
    private JwtAuthenticationToken authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private DiscussionController discussionController;

    private CommentResponseDto sampleResponse;
    private final Long CHALLENGE_ID = 1L;
    private final String USER_ID = "auth0|user123";
    private final String USER_NAME = "JohnDoe";

    @BeforeEach
    void setUp() {
        sampleResponse = CommentResponseDto.builder()
                .id(10L)
                .challengeId(CHALLENGE_ID)
                .userId(USER_ID)
                .userName(USER_NAME)
                .content("Test comment")
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/challenges/{challengeId}/comments")
    class GetComments {

        @Test
        @DisplayName("should return 200 and list of comments")
        void shouldReturnComments() {
            when(discussionService.getCommentsByChallenge(CHALLENGE_ID))
                    .thenReturn(List.of(sampleResponse));

            ResponseEntity<List<CommentResponseDto>> response = discussionController.getComments(CHALLENGE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getContent()).isEqualTo("Test comment");
        }
    }

    @Nested
    @DisplayName("POST /api/challenges/{challengeId}/comments")
    class AddComment {

        @Test
        @DisplayName("should create comment and resolve username from JWT when not provided in request")
        void shouldAddCommentWithResolvedName() {
            CommentRequestDto request = CommentRequestDto.builder()
                    .content("New comment")
                    .build();

            when(authentication.getToken()).thenReturn(jwt);
            when(jwt.getSubject()).thenReturn(USER_ID);
            when(jwt.getClaims()).thenReturn(Map.of("nickname", USER_NAME));
            when(jwt.getClaimAsString("nickname")).thenReturn(USER_NAME);
            
            when(discussionService.createComment(eq(CHALLENGE_ID), eq(request), eq(USER_ID), eq(USER_NAME)))
                    .thenReturn(sampleResponse);

            ResponseEntity<CommentResponseDto> response = discussionController.addComment(CHALLENGE_ID, request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(discussionService).createComment(CHALLENGE_ID, request, USER_ID, USER_NAME);
        }

        @Test
        @DisplayName("should use userName from request if provided")
        void shouldAddCommentWithRequestName() {
            CommentRequestDto request = CommentRequestDto.builder()
                    .content("New comment")
                    .userName("CustomName")
                    .build();

            when(authentication.getToken()).thenReturn(jwt);
            when(jwt.getSubject()).thenReturn(USER_ID);
            
            when(discussionService.createComment(eq(CHALLENGE_ID), eq(request), eq(USER_ID), eq("CustomName")))
                    .thenReturn(sampleResponse);

            ResponseEntity<CommentResponseDto> response = discussionController.addComment(CHALLENGE_ID, request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(discussionService).createComment(CHALLENGE_ID, request, USER_ID, "CustomName");
        }
    }

    @Nested
    @DisplayName("DELETE /api/challenges/comments/{commentId}")
    class DeleteComment {

        @Test
        @DisplayName("should delete and return 204")
        void shouldDeleteComment() {
            when(authentication.getToken()).thenReturn(jwt);
            when(jwt.getSubject()).thenReturn(USER_ID);

            ResponseEntity<Void> response = discussionController.deleteComment(10L, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(discussionService).deleteComment(10L, USER_ID);
        }
    }
}
