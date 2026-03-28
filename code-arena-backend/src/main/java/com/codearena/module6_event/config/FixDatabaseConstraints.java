package com.codearena.module6_event.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class FixDatabaseConstraints {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void executeFixes() {
        try {
            // Fix missing primary keys in codearena (5).sql which causes foreign key error 150 on startup.
            System.out.println("Applying primary keys to programming_events and event_registrations...");
            jdbcTemplate.execute("ALTER TABLE programming_events ADD PRIMARY KEY (id)");
        } catch (Exception e) {
            // Might throw if it already has a primary key
            // e.printStackTrace();
        }
        try {
            jdbcTemplate.execute("ALTER TABLE event_registrations ADD PRIMARY KEY (id)");
        } catch (Exception e) {
            // Might throw if it already has a primary key
            // e.printStackTrace();
        }
        
        try {
            // Ensure event_id is properly indexed to allow foreign keys
            jdbcTemplate.execute("CREATE INDEX idx_event_id ON programming_events(id)");
        } catch (Exception e) {}
    }
}
