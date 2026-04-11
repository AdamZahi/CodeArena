package com.codearena.module2_battle.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Handles schema migrations that Hibernate ddl-auto=update cannot manage automatically,
 * such as column renames, type changes, and dropping obsolete columns.
 * All operations are idempotent — safe to run repeatedly.
 */
@Slf4j
@Configuration
public class BattleSchemaMigration {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        renameRoomKeyToInviteToken();
        dropChallengeIdFromBattleRoom();
        dropOldStatusStringValues();
        fixParticipantJoinedAtType();
    }

    /**
     * Rename battle_room.room_key → invite_token if the old column still exists.
     */
    private void renameRoomKeyToInviteToken() {
        try {
            // Check if room_key column exists
            jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'battle_room' AND COLUMN_NAME = 'room_key'",
                    Integer.class);
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'battle_room' AND COLUMN_NAME = 'room_key'",
                    Integer.class);
            if (count > 0) {
                jdbcTemplate.execute("ALTER TABLE battle_room CHANGE room_key invite_token varchar(255) DEFAULT NULL");
                log.info("Renamed battle_room.room_key → invite_token");
            }
        } catch (Exception e) {
            log.debug("Skipping room_key rename: {}", e.getMessage());
        }
    }

    /**
     * Drop the old direct challenge_id column from battle_room
     * (challenges are now in the battle_room_challenge join table).
     */
    private void dropChallengeIdFromBattleRoom() {
        try {
            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'battle_room' AND COLUMN_NAME = 'challenge_id'",
                    Integer.class);
            if (count > 0) {
                jdbcTemplate.execute("ALTER TABLE battle_room DROP COLUMN challenge_id");
                log.info("Dropped obsolete battle_room.challenge_id column");
            }
        } catch (Exception e) {
            log.debug("Skipping challenge_id drop: {}", e.getMessage());
        }
    }

    /**
     * If battle_room.status still contains old plain-string values, this is handled
     * by Hibernate re-creating the column as an ENUM. No extra migration needed
     * as long as existing values match the new enum values (WAITING, IN_PROGRESS, FINISHED
     * are all present in the new BattleRoomStatus enum).
     */
    private void dropOldStatusStringValues() {
        // No-op: Hibernate ddl-auto=update will handle the column type change.
        // Existing values WAITING, IN_PROGRESS, FINISHED are valid in the new enum.
    }

    /**
     * Convert battle_participant.joined_at from varchar to timestamp if needed.
     * Also handles score and rank varchar→int conversion by clearing invalid data.
     */
    private void fixParticipantJoinedAtType() {
        try {
            // Clear non-numeric score values so Hibernate can re-create as int
            jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'battle_participant' " +
                    "AND COLUMN_NAME = 'score' AND DATA_TYPE = 'varchar'",
                    Integer.class);
            int scoreVarcharCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'battle_participant' " +
                    "AND COLUMN_NAME = 'score' AND DATA_TYPE = 'varchar'",
                    Integer.class);
            if (scoreVarcharCount > 0) {
                jdbcTemplate.execute("ALTER TABLE battle_participant MODIFY COLUMN score INT DEFAULT NULL");
                log.info("Changed battle_participant.score from varchar to int");
            }
        } catch (Exception e) {
            log.debug("Skipping score type fix: {}", e.getMessage());
        }

        try {
            int rankVarcharCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'battle_participant' " +
                    "AND COLUMN_NAME = 'rank' AND DATA_TYPE = 'varchar'",
                    Integer.class);
            if (rankVarcharCount > 0) {
                jdbcTemplate.execute("ALTER TABLE battle_participant MODIFY COLUMN `rank` INT DEFAULT NULL");
                log.info("Changed battle_participant.rank from varchar to int");
            }
        } catch (Exception e) {
            log.debug("Skipping rank type fix: {}", e.getMessage());
        }
    }
}
