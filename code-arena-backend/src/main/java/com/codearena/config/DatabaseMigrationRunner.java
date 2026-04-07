package com.codearena.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureColumnExists("coach_applications", "cv_file_base64", "LONGTEXT");
        ensureColumnExists("coach_applications", "cv_file_name", "VARCHAR(255)");
    }

    private void ensureColumnExists(String table, String column, String type) {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, table, column);
            if (count == null || count == 0) {
                String alterSql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
                jdbcTemplate.execute(alterSql);
                log.info("✅ Added missing column: {}.{} ({})", table, column, type);
            } else {
                log.info("Column {}.{} already exists.", table, column);
            }
        } catch (Exception e) {
            log.warn("Could not check/add column {}.{}: {}", table, column, e.getMessage());
        }
    }
}
