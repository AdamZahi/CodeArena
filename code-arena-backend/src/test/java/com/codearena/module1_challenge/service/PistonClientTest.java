package com.codearena.module1_challenge.service;

import com.codearena.module2_battle.dto.PistonExecutionRequest;
import com.codearena.module2_battle.dto.PistonExecutionResult;
import com.codearena.module2_battle.service.PistonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PistonClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PistonClient pistonClient;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        pistonClient = new PistonClient("http://192.168.0.195:2000");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(pistonClient, "restTemplate");
        assertThat(restTemplate).isNotNull();
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("execute should map a successful Piston response")
    void executeShouldMapSuccessfulResponse() throws Exception {
        Map<String, Object> body = Map.of(
                "run", Map.of(
                        "code", 0,
                        "stdout", "1",
                        "stderr", "",
                        "cpu_time", 42,
                        "memory", 1024
                ),
                "compile", Map.of(
                        "code", 0,
                        "stderr", "",
                        "output", ""
                )
        );

        server.expect(requestTo("http://192.168.0.195:2000/api/v2/execute"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(body), MediaType.APPLICATION_JSON));

        PistonExecutionResult result = pistonClient.execute(PistonExecutionRequest.builder()
                .language("python")
                .version("3.12.0")
                .sourceCode("print(1)")
                .stdin("1")
                .build());

        assertThat(result.getExitCode()).isEqualTo(0);
        assertThat(result.getStdout()).isEqualTo("1");
        assertThat(result.getCompileOutput()).isNull();
        assertThat(result.getCpuTimeMs()).isEqualTo(42);
        assertThat(result.getMemoryKb()).isEqualTo(1);

        server.verify();
    }

    @Test
    @DisplayName("execute should surface a top-level Piston error message")
    void executeShouldSurfaceErrorMessage() throws Exception {
        server.expect(requestTo("http://192.168.0.195:2000/api/v2/execute"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(Map.of("message", "unsupported language")), MediaType.APPLICATION_JSON));

        PistonExecutionResult result = pistonClient.execute(PistonExecutionRequest.builder()
                .language("brainfuck")
                .version("1.0.0")
                .sourceCode(">+.")
                .build());

        assertThat(result.getExitCode()).isEqualTo(1);
        assertThat(result.getCompileOutput()).isEqualTo("unsupported language");
        assertThat(result.getErrorMessage()).isEqualTo("unsupported language");

        server.verify();
    }
}