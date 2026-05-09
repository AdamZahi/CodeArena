package com.codearena.module1_challenge.ai.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiDatabaseSetup {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void setupDatabases() {
        log.info("Initializing AI engine database tables...");
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS challenge_difficulty_profile (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    challenge_id BIGINT NOT NULL,
                    ai_difficulty_score FLOAT NOT NULL DEFAULT 50.0,
                    pass_rate FLOAT DEFAULT 0.0,
                    avg_attempts FLOAT DEFAULT 0.0,
                    avg_execution_time FLOAT DEFAULT 0.0,
                    compilation_error_rate FLOAT DEFAULT 0.0,
                    wrong_answer_rate FLOAT DEFAULT 0.0,
                    tle_rate FLOAT DEFAULT 0.0,
                    sample_size INT DEFAULT 0,
                    last_calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE (challenge_id)
                )
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_skill_profile (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id VARCHAR(255) NOT NULL,
                    skill_vector TEXT,
                    overall_skill_rating FLOAT DEFAULT 0.0,
                    total_solved INT DEFAULT 0,
                    total_attempted INT DEFAULT 0,
                    last_calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE (user_id)
                )
            """);
            log.info("AI engine database tables are ready.");
        } catch (Exception e) {
            log.error("Failed to initialize AI engine tables: {}", e.getMessage());
        }
    }
}
