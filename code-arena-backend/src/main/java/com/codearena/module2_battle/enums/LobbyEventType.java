package com.codearena.module2_battle.enums;

public final class LobbyEventType {
    private LobbyEventType() {}

    public static final String LOBBY_STATE = "LOBBY_STATE";
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String PLAYER_LEFT = "PLAYER_LEFT";
    public static final String PLAYER_KICKED = "PLAYER_KICKED";
    public static final String READY_CHANGED = "READY_CHANGED";
    public static final String COUNTDOWN_STARTED = "COUNTDOWN_STARTED";
    public static final String BATTLE_STARTED = "BATTLE_STARTED";
    public static final String ROOM_CANCELLED = "ROOM_CANCELLED";

    // Arena-phase event types (Step 3)
    public static final String ARENA_STATE = "ARENA_STATE";
    public static final String SUBMISSION_RESULT = "SUBMISSION_RESULT";
    public static final String OPPONENT_PROGRESS = "OPPONENT_PROGRESS";
    public static final String SPECTATOR_FEED = "SPECTATOR_FEED";
    public static final String MATCH_FINISHED = "MATCH_FINISHED";
    public static final String MATCH_CANCELLED = "MATCH_CANCELLED";

    // Feature 1: Opponent activity
    public static final String OPPONENT_ACTIVITY = "OPPONENT_ACTIVITY";

    // Feature 2: Per-test-case progress
    public static final String TEST_CASE_PROGRESS = "TEST_CASE_PROGRESS";

    // Feature 3: Disconnect/reconnect
    public static final String PLAYER_DISCONNECTED = "PLAYER_DISCONNECTED";
    public static final String PLAYER_RECONNECTED = "PLAYER_RECONNECTED";
}
