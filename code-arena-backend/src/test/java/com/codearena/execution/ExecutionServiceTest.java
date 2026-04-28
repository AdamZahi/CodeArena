package com.codearena.execution;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock
    private PistonExecutionService pistonService;

    @Mock
    private Judge0ExecutionService judge0Service;

    @Mock
    private ExecutionConfig config;

    @Mock
    private ExecutionConfig.PistonConfig pistonConfig;

    private LanguageRegistry languageRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private FallbackExecutionService fallbackService;

    @BeforeEach
    void setUp() {
        languageRegistry = new LanguageRegistry();
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        lenient().when(config.getPiston()).thenReturn(pistonConfig);
        lenient().when(pistonConfig.isEnabled()).thenReturn(true);
        fallbackService = new FallbackExecutionService(
                pistonService, judge0Service, config, languageRegistry, circuitBreakerRegistry);
    }

    // ─── Test 1: Piston success path ───

    @Test
    @DisplayName("Should use Piston and return correct result when Piston succeeds")
    void pistonSuccess() {
        ExecutionRequest request = ExecutionRequest.builder()
                .sourceCode("print('hello')")
                .language("python")
                .stdin("")
                .build();

        ExecutionResult pistonResult = ExecutionResult.builder()
                .stdout("hello")
                .stderr("")
                .exitCode(0)
                .engineUsed("PISTON")
                .executionTimeMs(42)
                .build();

        when(pistonService.execute(any())).thenReturn(pistonResult);

        ExecutionResult result = fallbackService.execute(request);

        assertThat(result.getEngineUsed()).isEqualTo("PISTON");
        assertThat(result.getStdout()).isEqualTo("hello");
        assertThat(result.getExitCode()).isEqualTo(0);
        assertThat(result.getExecutionTimeMs()).isEqualTo(42);
        verify(judge0Service, never()).execute(any());
    }

    // ─── Test 2: Piston timeout → Judge0 fallback ───

    @Test
    @DisplayName("Should fallback to Judge0 when Piston throws SocketTimeoutException")
    void pistonTimeoutFallsBackToJudge0() {
        ExecutionRequest request = ExecutionRequest.builder()
                .sourceCode("System.out.println(1);")
                .language("java")
                .stdin("")
                .build();

        when(pistonService.execute(any()))
                .thenThrow(new ResourceAccessException("Timeout", new SocketTimeoutException("Read timed out")));

        ExecutionResult judge0Result = ExecutionResult.builder()
                .stdout("1")
                .stderr("")
                .exitCode(0)
                .engineUsed("JUDGE0")
                .executionTimeMs(500)
                .build();
        when(judge0Service.execute(any())).thenReturn(judge0Result);

        ExecutionResult result = fallbackService.execute(request);

        assertThat(result.getEngineUsed()).isEqualTo("JUDGE0");
        assertThat(result.getStdout()).isEqualTo("1");
        assertThat(result.getExitCode()).isEqualTo(0);
        verify(pistonService).execute(any());
        verify(judge0Service).execute(any());
    }

    // ─── Test 3: Piston connection refused → Judge0 fallback ───

    @Test
    @DisplayName("Should fallback to Judge0 when Piston throws ConnectException")
    void pistonConnectionRefusedFallsBackToJudge0() {
        ExecutionRequest request = ExecutionRequest.builder()
                .sourceCode("puts 'hi'")
                .language("ruby")
                .stdin("")
                .build();

        when(pistonService.execute(any()))
                .thenThrow(new ResourceAccessException("Connection refused", new ConnectException("Connection refused")));

        ExecutionResult judge0Result = ExecutionResult.builder()
                .stdout("hi")
                .stderr("")
                .exitCode(0)
                .engineUsed("JUDGE0")
                .executionTimeMs(300)
                .build();
        when(judge0Service.execute(any())).thenReturn(judge0Result);

        ExecutionResult result = fallbackService.execute(request);

        assertThat(result.getEngineUsed()).isEqualTo("JUDGE0");
        assertThat(result.getStdout()).isEqualTo("hi");
    }

    // ─── Test 4: Circuit breaker OPEN → direct Judge0 ───

    @Test
    @DisplayName("Should route directly to Judge0 when circuit breaker is OPEN")
    void circuitBreakerOpenRoutesToJudge0() {
        // Force the circuit breaker to OPEN state
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pistonEngine");
        cb.transitionToOpenState();

        ExecutionRequest request = ExecutionRequest.builder()
                .sourceCode("console.log('test')")
                .language("javascript")
                .stdin("")
                .build();

        ExecutionResult judge0Result = ExecutionResult.builder()
                .stdout("test")
                .stderr("")
                .exitCode(0)
                .engineUsed("JUDGE0")
                .executionTimeMs(200)
                .build();
        when(judge0Service.execute(any())).thenReturn(judge0Result);

        ExecutionResult result = fallbackService.execute(request);

        assertThat(result.getEngineUsed()).isEqualTo("JUDGE0");
        // Piston should NEVER be called when circuit is open
        verify(pistonService, never()).execute(any());
        verify(judge0Service).execute(any());
    }

    // ─── Test 5: Unsupported language passthrough ───

    @Test
    @DisplayName("Should route directly to Judge0 for languages not supported by Piston")
    void unsupportedLanguagePassthrough() {
        ExecutionRequest request = ExecutionRequest.builder()
                .sourceCode("(print 'hello)")
                .language("lisp")  // Not in Piston manifest
                .stdin("")
                .build();

        ExecutionResult judge0Result = ExecutionResult.builder()
                .stdout("hello")
                .stderr("")
                .exitCode(0)
                .engineUsed("JUDGE0")
                .executionTimeMs(100)
                .build();
        when(judge0Service.execute(any())).thenReturn(judge0Result);

        ExecutionResult result = fallbackService.execute(request);

        assertThat(result.getEngineUsed()).isEqualTo("JUDGE0");
        // Piston should NEVER be called for unsupported languages
        verify(pistonService, never()).execute(any());
    }

    // ─── Test 6: Language registry resolution ───

    @Test
    @DisplayName("Language registry should resolve numeric IDs and aliases correctly")
    void languageRegistryResolution() {
        // Numeric Judge0 ID
        LanguageRegistry.LanguageMapping java = languageRegistry.resolve("62");
        assertThat(java.isPistonSupported()).isTrue();
        assertThat(java.pistonLanguage()).isEqualTo("java");
        assertThat(java.pistonVersion()).isEqualTo("15.0.2");
        assertThat(java.judge0Id()).isEqualTo(62);

        // Alias "py" → python
        LanguageRegistry.LanguageMapping python = languageRegistry.resolve("py");
        assertThat(python.isPistonSupported()).isTrue();
        assertThat(python.pistonLanguage()).isEqualTo("python");
        assertThat(python.pistonVersion()).isEqualTo("3.12.0");
        assertThat(python.judge0Id()).isEqualTo(71);

        // Alias "js" → javascript
        LanguageRegistry.LanguageMapping js = languageRegistry.resolve("js");
        assertThat(js.isPistonSupported()).isTrue();
        assertThat(js.pistonLanguage()).isEqualTo("javascript");

        // Alias "cpp" → c++
        LanguageRegistry.LanguageMapping cpp = languageRegistry.resolve("cpp");
        assertThat(cpp.isPistonSupported()).isTrue();
        assertThat(cpp.pistonLanguage()).isEqualTo("c++");

        // Unknown language
        LanguageRegistry.LanguageMapping unknown = languageRegistry.resolve("brainfuck");
        assertThat(unknown.isPistonSupported()).isFalse();
    }

    // ─── Test 7: Piston disabled by config ───

    @Test
    @DisplayName("Should route directly to Judge0 when Piston is disabled in config")
    void pistonDisabledRoutesToJudge0() {
        when(pistonConfig.isEnabled()).thenReturn(false);

        ExecutionRequest request = ExecutionRequest.builder()
                .sourceCode("print(1)")
                .language("python")
                .stdin("")
                .build();

        ExecutionResult judge0Result = ExecutionResult.builder()
                .stdout("1")
                .stderr("")
                .exitCode(0)
                .engineUsed("JUDGE0")
                .executionTimeMs(150)
                .build();
        when(judge0Service.execute(any())).thenReturn(judge0Result);

        ExecutionResult result = fallbackService.execute(request);

        assertThat(result.getEngineUsed()).isEqualTo("JUDGE0");
        verify(pistonService, never()).execute(any());
    }
}
