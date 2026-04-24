package com.codearena.module1_challenge.controller;

import com.codearena.module1_challenge.dto.SubmissionDto;
import com.codearena.module1_challenge.dto.SubmitCodeRequest;
import com.codearena.module1_challenge.service.SubmissionService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    @Mock
    private SubmissionService submissionService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private SubmissionController submissionController;

    private SubmissionDto sampleSubmission;
    private final String USER_ID = "auth0|user123";

    @BeforeEach
    void setUp() {
        sampleSubmission = SubmissionDto.builder()
                .id(100L)
                .challengeId(1L)
                .userId(USER_ID)
                .code("int x = 1;")
                .language("JAVA")
                .status("PENDING")
                .submittedAt(Instant.now())
                .challengeTitle("Two Sum")
                .build();
    }

    @Nested
    @DisplayName("POST /api/submissions")
    class SubmitCode {

        @Test
        @DisplayName("should submit code and return 200 with the submission DTO")
        void shouldSubmitCode() {
            SubmitCodeRequest request = SubmitCodeRequest.builder()
                    .challengeId(1L).code("int x = 1;").language("JAVA").build();

            when(jwt.getSubject()).thenReturn(USER_ID);
            when(submissionService.submitCode(request, USER_ID)).thenReturn(sampleSubmission);

            ResponseEntity<SubmissionDto> response = submissionController.submitCode(request, jwt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo("PENDING");
            verify(submissionService).submitCode(request, USER_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/submissions/{id}")
    class GetSubmissionStatus {

        @Test
        @DisplayName("should return the submission status")
        void shouldReturnStatus() {
            when(submissionService.getSubmissionStatus(100L)).thenReturn(sampleSubmission);

            ResponseEntity<SubmissionDto> response = submissionController.getSubmissionStatus(100L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("GET /api/submissions/me")
    class GetMySubmissions {

        @Test
        @DisplayName("should return current user's submissions")
        void shouldReturnMySubmissions() {
            when(jwt.getSubject()).thenReturn(USER_ID);
            when(submissionService.getUserSubmissions(USER_ID)).thenReturn(List.of(sampleSubmission));

            ResponseEntity<List<SubmissionDto>> response = submissionController.getMySubmissions(jwt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("GET /api/submissions/user/{userId}")
    class GetUserSubmissions {

        @Test
        @DisplayName("should return submissions for a specific user (admin endpoint)")
        void shouldReturnUserSubmissions() {
            when(jwt.getSubject()).thenReturn("admin-user");
            when(jwt.getClaimAsStringList("https://codearena.com/roles")).thenReturn(List.of("ADMIN"));
            when(submissionService.getUserSubmissions("target-user")).thenReturn(List.of(sampleSubmission));

            ResponseEntity<List<SubmissionDto>> response = submissionController.getUserSubmissions("target-user", jwt);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(submissionService).getUserSubmissions("target-user");
        }
    }
}
