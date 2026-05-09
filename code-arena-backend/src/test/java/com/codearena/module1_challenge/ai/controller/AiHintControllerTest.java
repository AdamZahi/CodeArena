package com.codearena.module1_challenge.ai.controller;

import com.codearena.module1_challenge.ai.dto.HintResponseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiHintControllerTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiHintController aiHintController;

    private Challenge challenge;
    private final Long CHALLENGE_ID = 1L;

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder()
                .id(CHALLENGE_ID)
                .title("Two Sum")
                .description("Desc")
                .difficulty("EASY")
                .tags("Array")
                .build();
        
        // Inject the mock RestTemplate because it's instantiated via 'new' in the controller
        ReflectionTestUtils.setField(aiHintController, "restTemplate", restTemplate);
    }

    @Nested
    @DisplayName("GET /api/ai/challenge/{id}/hint")
    class GenerateAiHint {

        @Test
        @DisplayName("should return 200 and hint from AI service")
        void shouldReturnHint() {
            HintResponseDto hintResponse = new HintResponseDto();
            hintResponse.setHint("Try using a HashMap.");

            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(restTemplate.postForEntity(anyString(), any(), eq(HintResponseDto.class)))
                    .thenReturn(new ResponseEntity<>(hintResponse, HttpStatus.OK));

            ResponseEntity<HintResponseDto> response = aiHintController.generateAiHint(CHALLENGE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getHint()).isEqualTo("Try using a HashMap.");
            verify(restTemplate).postForEntity(contains("/predict-hint"), any(), eq(HintResponseDto.class));
        }

        @Test
        @DisplayName("should return 404 when challenge not found")
        void shouldReturn404() {
            when(challengeRepository.findById(999L)).thenReturn(Optional.empty());

            ResponseEntity<HintResponseDto> response = aiHintController.generateAiHint(999L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("should return 500 when AI service fails")
        void shouldReturn500OnFailure() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(restTemplate.postForEntity(anyString(), any(), any()))
                    .thenThrow(new RuntimeException("AI DOWN"));

            ResponseEntity<HintResponseDto> response = aiHintController.generateAiHint(CHALLENGE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
