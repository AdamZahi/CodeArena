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

-- Backoffice Battle Module: global configuration (singleton row, id=1)
CREATE TABLE IF NOT EXISTS battle_config (
    id                                  INT PRIMARY KEY AUTO_INCREMENT,
    max_participants                    INT NOT NULL DEFAULT 2,
    time_limit_minutes                  INT NOT NULL DEFAULT 30,
    allowed_languages                   TEXT NOT NULL,
    xp_reward_winner                    INT NOT NULL DEFAULT 100,
    xp_reward_loser                     INT NOT NULL DEFAULT 20,
    min_rank_required                   VARCHAR(50) DEFAULT NULL,
    allow_spectators                    BOOLEAN NOT NULL DEFAULT FALSE,
    auto_close_abandoned_after_minutes  INT NOT NULL DEFAULT 10,
    updated_at                          DATETIME NOT NULL,
    updated_by                          VARCHAR(255) NOT NULL
);

-- Seed the singleton row if no config exists yet
INSERT INTO battle_config (
    max_participants, time_limit_minutes, allowed_languages,
    xp_reward_winner, xp_reward_loser, min_rank_required,
    allow_spectators, auto_close_abandoned_after_minutes,
    updated_at, updated_by
)
SELECT 2, 30, '["java","python","cpp","javascript","typescript"]',
       100, 20, NULL, FALSE, 10, NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM battle_config);

-- Backoffice Battle Module: admin action audit log
CREATE TABLE IF NOT EXISTS battle_audit_log (
    id              BINARY(16) PRIMARY KEY,
    admin_id        VARCHAR(255) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    target_room_id  VARCHAR(255),
    details         TEXT,
    performed_at    DATETIME NOT NULL,
    INDEX idx_audit_admin_id (admin_id),
    INDEX idx_audit_performed_at (performed_at),
    INDEX idx_audit_target_room (target_room_id),
    INDEX idx_audit_action (action)
);
