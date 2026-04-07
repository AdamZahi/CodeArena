package com.codearena.module2_battle.service;

import com.codearena.module2_battle.config.TimeLimitProperties;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.event.BattleRoomStatusChangedEvent;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Automatically terminates matches that exceed their time limit.
 * Listens for IN_PROGRESS transitions to schedule forced finish,
 * and cancels the timer when a room finishes early.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchTimeLimitScheduler {

    private final TaskScheduler taskScheduler;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleRoomStateMachineService stateMachineService;
    private final TimeLimitProperties timeLimitProperties;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    /**
     * Listens for IN_PROGRESS transitions to schedule the time limit timer.
     */
    @EventListener
    public void onBattleStarted(BattleRoomStatusChangedEvent event) {
        if (event.getNewStatus() != BattleRoomStatus.IN_PROGRESS) {
            return;
        }

        String roomId = event.getRoomId();
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId)).orElse(null);
        if (room == null) return;

        int limitMinutes = getTimeLimitMinutes(room.getMode());
        if (limitMinutes <= 0) {
            log.debug("No time limit for room {} (mode: {})", roomId, room.getMode());
            return;
        }

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> forceFinishRoom(roomId),
                Instant.now().plusSeconds((long) limitMinutes * 60)
        );
        timers.put(roomId, future);
        log.info("Scheduled time limit of {} minutes for room {} (mode: {})", limitMinutes, roomId, room.getMode());
    }

    /**
     * Forces a room to finish when the time limit expires.
     * Guards against race conditions: if the room is already FINISHED or CANCELLED, does nothing.
     */
    private void forceFinishRoom(String roomId) {
        timers.remove(roomId);
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId)).orElse(null);
        if (room == null) return;

        // Race condition guard — room may have finished before the timer fired
        if (room.getStatus() == BattleRoomStatus.FINISHED || room.getStatus() == BattleRoomStatus.CANCELLED) {
            log.debug("Room {} already {} — skipping forced finish", roomId, room.getStatus());
            return;
        }

        log.info("Time limit reached for room {} — forcing transition to FINISHED", roomId);
        stateMachineService.transitionToFinished(roomId);
    }

    /**
     * Cancels the scheduled timer for a room.
     * Called when a room finishes early (all players done) or is cancelled.
     */
    public void cancelTimer(String roomId) {
        ScheduledFuture<?> future = timers.remove(roomId);
        if (future != null) {
            future.cancel(false);
            log.debug("Cancelled time limit timer for room {}", roomId);
        }
    }

    private int getTimeLimitMinutes(BattleMode mode) {
        return switch (mode) {
            case DUEL -> timeLimitProperties.getDuelMinutes();
            case TEAM -> timeLimitProperties.getTeamMinutes();
            case RANKED_ARENA -> timeLimitProperties.getRankedArenaMinutes();
            case BLITZ -> timeLimitProperties.getBlitzMinutes();
            case PRACTICE -> timeLimitProperties.getPracticeMinutes();
            case DAILY -> timeLimitProperties.getDailyMinutes();
        };
    }
}
