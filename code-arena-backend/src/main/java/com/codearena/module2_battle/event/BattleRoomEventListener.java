package com.codearena.module2_battle.event;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.service.ArenaBroadcastService;
import com.codearena.module2_battle.service.BattleArenaService;
import com.codearena.module2_battle.service.BattleScoringService;
import com.codearena.module2_battle.service.LobbyBroadcastService;
import com.codearena.module2_battle.service.MatchTimeLimitScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Bridges Spring domain events from BattleRoomStateMachineService into WebSocket broadcasts.
 * Handles both lobby-phase and arena-phase status transitions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleRoomEventListener {

    private final LobbyBroadcastService lobbyBroadcastService;
    private final ArenaBroadcastService arenaBroadcastService;
    private final BattleArenaService battleArenaService;
    private final BattleScoringService battleScoringService;
    private final MatchTimeLimitScheduler matchTimeLimitScheduler;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository participantRepository;

    @EventListener
    public void onStatusChanged(BattleRoomStatusChangedEvent event) {
        String roomId = event.getRoomId();

        switch (event.getNewStatus()) {
            case IN_PROGRESS -> {
                BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId)).orElse(null);
                if (room != null) {
                    BattleRoomResponse response = buildBattleRoomResponse(room);
                    lobbyBroadcastService.broadcastBattleStarted(roomId, response);
                }
            }
            case CANCELLED -> {
                lobbyBroadcastService.broadcastRoomCancelled(roomId, "Room cancelled");
                arenaBroadcastService.broadcastMatchCancelled(roomId, "Room cancelled");
                matchTimeLimitScheduler.cancelTimer(roomId);
            }
            case FINISHED -> {
                // Step 4: compute scores first, then broadcast with results
                PostMatchSummaryResponse summary = battleScoringService.processMatchResults(roomId);
                MatchFinishedEvent finishedEvent = buildMatchFinishedEvent(roomId, summary);
                arenaBroadcastService.broadcastMatchFinished(roomId, finishedEvent);
                matchTimeLimitScheduler.cancelTimer(roomId);
            }
            // COUNTDOWN is handled directly in BattleRoomService.startBattle
            default -> log.debug("No WebSocket broadcast for transition to {}", event.getNewStatus());
        }
    }

    /**
     * Builds the MatchFinishedEvent with final standings from scored results.
     * Uses PostMatchSummaryResponse from BattleScoringService which includes final scores and ranks.
     */
    private MatchFinishedEvent buildMatchFinishedEvent(String roomId, PostMatchSummaryResponse summary) {
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId)).orElse(null);

        // Convert PlayerScoreResponse standings to ArenaParticipantProgressResponse
        List<ArenaParticipantProgressResponse> standings = summary.getStandings().stream()
                .map(ps -> ArenaParticipantProgressResponse.builder()
                        .participantId(ps.getParticipantId())
                        .userId(ps.getUserId())
                        .username(ps.getUsername())
                        .avatarUrl(ps.getAvatarUrl())
                        .challengesCompleted((int) ps.getChallengeBreakdowns().stream()
                                .filter(ScoreBreakdownResponse::isSolved).count())
                        .currentChallengePosition(0)
                        .totalAttempts(ps.getChallengeBreakdowns().stream()
                                .mapToInt(ScoreBreakdownResponse::getAttemptCount).sum())
                        .isFinished(ps.getChallengeBreakdowns().stream()
                                .allMatch(ScoreBreakdownResponse::isSolved))
                        .build())
                .toList();

        return MatchFinishedEvent.builder()
                .roomId(roomId)
                .triggerReason(summary.getFinishReason())
                .finishedAt(room != null ? room.getEndsAt() : LocalDateTime.now())
                .finalStandings(standings)
                .build();
    }

    private BattleRoomResponse buildBattleRoomResponse(BattleRoom room) {
        String roomIdStr = room.getId().toString();
        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(roomIdStr, ParticipantRole.PLAYER);
        int spectatorCount = participantRepository
                .countByRoomIdAndRole(roomIdStr, ParticipantRole.SPECTATOR);

        List<ParticipantResponse> participantResponses = players.stream()
                .map(p -> ParticipantResponse.builder()
                        .participantId(p.getId().toString())
                        .userId(p.getUserId())
                        .role(p.getRole().name())
                        .isReady(Boolean.TRUE.equals(p.getIsReady()))
                        .build())
                .toList();

        LocalDateTime createdAt = room.getCreatedAt() != null
                ? LocalDateTime.ofInstant(room.getCreatedAt(), ZoneId.systemDefault())
                : null;

        return BattleRoomResponse.builder()
                .id(roomIdStr)
                .mode(room.getMode().name())
                .status(room.getStatus().name())
                .maxPlayers(room.getMaxPlayers())
                .challengeCount(room.getChallengeCount())
                .isPublic(Boolean.TRUE.equals(room.getIsPublic()))
                .inviteToken(room.getInviteToken())
                .hostId(room.getHostId())
                .createdAt(createdAt)
                .startsAt(room.getStartsAt())
                .participants(participantResponses)
                .spectatorCount(spectatorCount)
                .build();
    }
}
