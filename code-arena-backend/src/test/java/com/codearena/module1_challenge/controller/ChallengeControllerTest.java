package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.ChallengeDto;
import com.codearena.module1_challenge.dto.CreateChallengeRequest;
import com.codearena.module1_challenge.service.ChallengeService;
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
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeControllerTest {

    @Mock
    private ChallengeService challengeService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private ChallengeController challengeController;

    private ChallengeDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = ChallengeDto.builder()
                .id(1L)
                .title("Two Sum")
                .description("Given an array...")
                .difficulty("EASY")
                .tags("Array, Hash Table")
                .language("Java")
                .authorId("auth0|admin")
                .createdAt(Instant.now())
                .testCases(Collections.emptyList())
                .build();
    }

    @Nested
    @DisplayName("GET /api/challenges")
    class GetAllChallenges {

        @Test
        @DisplayName("should return 200 with list of challenges")
        void shouldReturnAllChallenges() {
            when(challengeService.getAllChallenges()).thenReturn(List.of(sampleDto));

            ResponseEntity<List<ChallengeDto>> response = challengeController.getAllChallenges();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getTitle()).isEqualTo("Two Sum");
        }

        @Test
        @DisplayName("should return 200 with empty list when no challenges exist")
        void shouldReturnEmptyList() {
            when(challengeService.getAllChallenges()).thenReturn(Collections.emptyList());

            ResponseEntity<List<ChallengeDto>> response = challengeController.getAllChallenges();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/challenges/{id}")
    class GetChallengeById {

        @Test
        @DisplayName("should return 200 with the challenge")
        void shouldReturnChallenge() {
            when(challengeService.getChallengeById(1L)).thenReturn(sampleDto);

            ResponseEntity<ChallengeDto> response = challengeController.getChallengeById(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTitle()).isEqualTo("Two Sum");
        }
    }

    @Nested
    @DisplayName("POST /api/challenges")
    class CreateChallenge {

        @Test
        @DisplayName("should create a challenge and return 200")
        void shouldCreateChallenge() {
            CreateChallengeRequest request = CreateChallengeRequest.builder()
                    .title("New")
                    .description("desc")
                    .difficulty("MEDIUM")
                    .build();

            when(jwt.getSubject()).thenReturn("auth0|admin");
            when(challengeService.createChallenge(any(), eq("auth0|admin"))).thenReturn(sampleDto);

            ResponseEntity<ChallengeDto> response = challengeController.createChallenge(request, jwt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(challengeService).createChallenge(request, "auth0|admin");
        }
    }

    @Nested
    @DisplayName("PUT /api/challenges/{id}")
    class UpdateChallenge {

        @Test
        @DisplayName("should update and return the challenge")
        void shouldUpdateChallenge() {
            CreateChallengeRequest request = CreateChallengeRequest.builder()
                    .title("Updated").build();

            when(challengeService.updateChallenge(1L, request)).thenReturn(sampleDto);

            ResponseEntity<ChallengeDto> response = challengeController.updateChallenge(1L, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(challengeService).updateChallenge(1L, request);
        }
    }

    @Nested
    @DisplayName("DELETE /api/challenges/{id}")
    class DeleteChallenge {

        @Test
        @DisplayName("should delete and return 200")
        void shouldDeleteChallenge() {
            ResponseEntity<Void> response = challengeController.deleteChallenge(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(challengeService).deleteChallenge(1L);
        }
    }
}
