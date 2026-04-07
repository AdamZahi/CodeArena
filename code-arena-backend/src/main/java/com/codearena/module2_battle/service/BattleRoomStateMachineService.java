package com.codearena.module2_battle.service;

import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.event.BattleRoomStatusChangedEvent;
import com.codearena.module2_battle.exception.BattleRoomNotFoundException;
import com.codearena.module2_battle.exception.InvalidRoomTransitionException;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The single authoritative place for battle_room.status transitions.
 * No other code should write to battle_room.status directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleRoomStateMachineService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * WAITING → COUNTDOWN
     * Preconditions: requesting user is the host, all players are ready, and at least 2 players exist.
     */
    @Transactional
    public BattleRoom transitionToCountdown(String roomId, String requestingUserId) {
        BattleRoom room = loadRoom(roomId);
        validateTransition(room, BattleRoomStatus.WAITING, BattleRoomStatus.COUNTDOWN);

        // Only the host can start the countdown
        if (!room.getHostId().equals(requestingUserId)) {
            throw new InvalidRoomTransitionException(
                    BattleRoomStatus.WAITING, BattleRoomStatus.COUNTDOWN,
                    "Only the host can start the countdown. userId=" + requestingUserId);
        }

        // At least 2 players must be in the room
        int playerCount = battleParticipantRepository.countByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        if (playerCount < 2) {
            throw new InvalidRoomTransitionException(
                    BattleRoomStatus.WAITING, BattleRoomStatus.COUNTDOWN,
                    "At least 2 players required, found " + playerCount);
        }

        // All players must be ready
        var players = battleParticipantRepository.findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        boolean allReady = players.stream().allMatch(p -> Boolean.TRUE.equals(p.getIsReady()));
        if (!allReady) {
            throw new InvalidRoomTransitionException(
                    BattleRoomStatus.WAITING, BattleRoomStatus.COUNTDOWN,
                    "Not all players are ready");
        }

        return applyTransition(room, BattleRoomStatus.COUNTDOWN);
    }

    /**
     * COUNTDOWN → IN_PROGRESS
     * Called when the countdown timer elapses (default 5 seconds).
     */
    @Transactional
    public BattleRoom transitionToInProgress(String roomId) {
        BattleRoom room = loadRoom(roomId);
        validateTransition(room, BattleRoomStatus.COUNTDOWN, BattleRoomStatus.IN_PROGRESS);

        room.setStartsAt(LocalDateTime.now());
        return applyTransition(room, BattleRoomStatus.IN_PROGRESS);
    }

    /**
     * IN_PROGRESS → FINISHED
     * Called when all players have submitted all challenges or the global time limit is reached.
     */
    @Transactional
    public BattleRoom transitionToFinished(String roomId) {
        BattleRoom room = loadRoom(roomId);
        validateTransition(room, BattleRoomStatus.IN_PROGRESS, BattleRoomStatus.FINISHED);

        room.setEndsAt(LocalDateTime.now());
        return applyTransition(room, BattleRoomStatus.FINISHED);
    }

    /**
     * WAITING/COUNTDOWN/IN_PROGRESS → CANCELLED
     * Allowed from WAITING, COUNTDOWN, or IN_PROGRESS.
     */
    @Transactional
    public BattleRoom transitionToCancelled(String roomId, String reason) {
        BattleRoom room = loadRoom(roomId);

        BattleRoomStatus current = room.getStatus();
        if (current == BattleRoomStatus.FINISHED || current == BattleRoomStatus.CANCELLED) {
            throw new InvalidRoomTransitionException(current, BattleRoomStatus.CANCELLED,
                    "Cannot cancel a room that is already " + current + ". reason=" + reason);
        }

        room.setEndsAt(LocalDateTime.now());
        log.info("Cancelling room {} from status {}. Reason: {}", roomId, current, reason);
        return applyTransition(room, BattleRoomStatus.CANCELLED);
    }

    private BattleRoom loadRoom(String roomId) {
        return battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));
    }

    private void validateTransition(BattleRoom room, BattleRoomStatus expectedCurrent, BattleRoomStatus target) {
        if (room.getStatus() != expectedCurrent) {
            throw new InvalidRoomTransitionException(room.getStatus(), target);
        }
    }

    private BattleRoom applyTransition(BattleRoom room, BattleRoomStatus newStatus) {
        BattleRoomStatus previousStatus = room.getStatus();
        room.setStatus(newStatus);
        BattleRoom saved = battleRoomRepository.save(room);

        eventPublisher.publishEvent(new BattleRoomStatusChangedEvent(
                saved.getId().toString(), previousStatus, newStatus));

        log.info("Room {} transitioned: {} → {}", saved.getId(), previousStatus, newStatus);
        return saved;
    }
}
