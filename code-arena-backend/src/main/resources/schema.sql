ALTER TABLE programming_events ADD PRIMARY KEY (id);
ALTER TABLE event_registrations ADD PRIMARY KEY (id);

-- Feature 3: Shareable Post-Match Result Card
CREATE TABLE IF NOT EXISTS shared_result (
    share_token VARCHAR(8) NOT NULL PRIMARY KEY,
    battle_room_id VARCHAR(255) NOT NULL,
    requested_by_user_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    INDEX idx_shared_result_room_user (battle_room_id, requested_by_user_id),
    INDEX idx_shared_result_expires (expires_at)
);
