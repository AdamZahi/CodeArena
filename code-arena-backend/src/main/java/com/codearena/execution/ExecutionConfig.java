package com.codearena.execution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for code execution engines.
 * Binds to {@code execution.piston.*} and {@code execution.judge0.*} in application.yml.
 * Provides a shared RestTemplate bean with configured timeouts.
 */
@Configuration
@ConfigurationProperties(prefix = "execution")
public class ExecutionConfig {

    private PistonConfig piston = new PistonConfig();
    private Judge0Config judge0 = new Judge0Config();

    public PistonConfig getPiston() {
        return piston;
    }

    public void setPiston(PistonConfig piston) {
        this.piston = piston;
    }

    public Judge0Config getJudge0() {
        return judge0;
    }

    public void setJudge0(Judge0Config judge0) {
        this.judge0 = judge0;
    }

    @Bean("executionRestTemplate")
    public RestTemplate executionRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Use the larger of the two timeouts for the shared RestTemplate
        int timeoutMs = Math.max(piston.getTimeoutSeconds(), judge0.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    public static class PistonConfig {
        private boolean enabled = true;
        private String baseUrl = "http://192.168.0.163:2000/api/v2";
        private int timeoutSeconds = 10;
        private int compileTimeoutMs = 10000;
        private int runTimeoutMs = 10000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public int getCompileTimeoutMs() { return compileTimeoutMs; }
        public void setCompileTimeoutMs(int compileTimeoutMs) { this.compileTimeoutMs = compileTimeoutMs; }
        public int getRunTimeoutMs() { return runTimeoutMs; }
        public void setRunTimeoutMs(int runTimeoutMs) { this.runTimeoutMs = runTimeoutMs; }
    }

    public static class Judge0Config {
        private String baseUrl = "https://ce.judge0.com";
        private String apiKey = "";
        private int timeoutSeconds = 10;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}
