package com.codearena.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock
    private PistonExecutionService pistonService;

    private LanguageRegistry languageRegistry;
    private FallbackExecutionService fallbackService;

    @BeforeEach
    void setUp() {
        languageRegistry = new LanguageRegistry();
                fallbackService = new FallbackExecutionService(pistonService);
    }

    @Test
        @DisplayName("Should use Piston and return correct result when execution succeeds")
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
        verify(pistonService).execute(any());
    }

    @Test
    @DisplayName("Language registry should resolve aliases correctly")
    void languageRegistryResolution() {
        LanguageRegistry.LanguageMapping java = languageRegistry.resolve("62");
        assertThat(java.isPistonSupported()).isTrue();
        assertThat(java.pistonLanguage()).isEqualTo("java");
        assertThat(java.pistonVersion()).isEqualTo("15.0.2");
        assertThat(java.judge0Id()).isEqualTo(62);

        LanguageRegistry.LanguageMapping python = languageRegistry.resolve("py");
        assertThat(python.isPistonSupported()).isTrue();
        assertThat(python.pistonLanguage()).isEqualTo("python");
        assertThat(python.pistonVersion()).isEqualTo("3.12.0");
        assertThat(python.judge0Id()).isEqualTo(71);

        LanguageRegistry.LanguageMapping js = languageRegistry.resolve("js");
        assertThat(js.isPistonSupported()).isTrue();
        assertThat(js.pistonLanguage()).isEqualTo("javascript");

        LanguageRegistry.LanguageMapping cpp = languageRegistry.resolve("cpp");
        assertThat(cpp.isPistonSupported()).isTrue();
        assertThat(cpp.pistonLanguage()).isEqualTo("c++");

        LanguageRegistry.LanguageMapping unknown = languageRegistry.resolve("brainfuck");
        assertThat(unknown.isPistonSupported()).isFalse();
    }
}
