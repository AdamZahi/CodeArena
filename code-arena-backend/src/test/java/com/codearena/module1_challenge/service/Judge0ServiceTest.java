package com.codearena.module1_challenge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Judge0ServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private Judge0Service judge0Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(judge0Service, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("submit should return token on success")
    void submitShouldReturnToken() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", "test-token-123");

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.CREATED));

        String token = judge0Service.submit("print(1)", "62", "1", "0");

        assertThat(token).isEqualTo("test-token-123");
        verify(restTemplate).postForEntity(contains("base64_encoded=true"), any(), eq(Map.class));
    }

    @Test
    @DisplayName("submit should return null on error")
    void submitShouldReturnNullOnFailure() {
        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new RuntimeException("API Down"));

        String token = judge0Service.submit("print(1)", "62", null, null);

        assertThat(token).isNull();
    }

    @Test
    @DisplayName("getSubmissionStatus should return result map")
    void getStatusShouldReturnMap() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", Map.of("id", 3, "description", "Accepted"));
        responseBody.put("stdout", "NDI="); // "42" in base64

        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        Map<String, Object> result = judge0Service.getSubmissionStatus("token123");

        assertThat(result).isNotNull();
        assertThat(result.get("stdout")).isEqualTo("NDI=");
    }

    @Test
    @DisplayName("decodeBase64 should correctly decode or return original if fail")
    void decodeBase64Tests() {
        assertThat(judge0Service.decodeBase64("SGVsbG8=")).isEqualTo("Hello");
        assertThat(judge0Service.decodeBase64(null)).isEqualTo("");
        assertThat(judge0Service.decodeBase64("invalid!@#")).isEqualTo("invalid!@#");
    }
}
