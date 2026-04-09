package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.entity.SharedResult;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.exception.BattleRoomNotFoundException;
import com.codearena.module2_battle.exception.ParticipantNotFoundException;
import com.codearena.module2_battle.exception.ResultsNotReadyException;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.SharedResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedResultService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TOKEN_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SharedResultRepository sharedResultRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleScoringService scoringService;

    @Value("${codearena.base-url:https://codearena.app}")
    private String baseUrl;

    @Transactional
    public ShareUrlResponse createOrGetShareToken(String roomId, String userId) {
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));

        if (room.getStatus() != BattleRoomStatus.FINISHED) {
            throw new ResultsNotReadyException(roomId, room.getStatus());
        }

        // Caller must be a participant of the room
        participantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        // Idempotent: return existing token if present
        Optional<SharedResult> existing = sharedResultRepository
                .findByBattleRoomIdAndRequestedByUserId(roomId, userId);
        if (existing.isPresent()) {
            return new ShareUrlResponse(buildShareUrl(existing.get().getShareToken()));
        }

        Instant now = Instant.now();
        SharedResult shared = SharedResult.builder()
                .shareToken(generateUniqueToken())
                .battleRoomId(roomId)
                .requestedByUserId(userId)
                .createdAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .build();
        sharedResultRepository.save(shared);

        return new ShareUrlResponse(buildShareUrl(shared.getShareToken()));
    }

    @Transactional(readOnly = true)
    public SharedResultDTO getSharedResult(String token) {
        SharedResult shared = sharedResultRepository.findById(token)
                .orElseThrow(() -> new NoSuchElementException("Shared result not found or expired"));

        if (shared.getExpiresAt().isBefore(Instant.now())) {
            throw new NoSuchElementException("Shared result has expired");
        }

        BattleRoom room = battleRoomRepository.findById(UUID.fromString(shared.getBattleRoomId()))
                .orElseThrow(() -> new BattleRoomNotFoundException(shared.getBattleRoomId()));

        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(shared.getBattleRoomId(), ParticipantRole.PLAYER);

        PostMatchSummaryResponse summary = scoringService.buildPostMatchSummary(room, players);

        // Map to public-safe DTO (no email/userId/absolute ELO/code)
        List<SharedParticipantResult> standings = summary.getStandings().stream().map(s -> {
            int solved = (int) s.getChallengeBreakdowns().stream()
                    .filter(b -> b.isSolved()).count();
            return new SharedParticipantResult(
                    s.getUsername(),
                    s.getFinalRank(),
                    s.getFinalScore(),
                    solved,
                    summary.getChallengeCount(),
                    s.getBadgesAwarded(),
                    isRanked(summary.getMode()) ? s.getEloChange() : null,
                    isRanked(summary.getMode()) ? s.getNewTier() : null
            );
        }).toList();

        Instant finishedAt = room.getEndsAt() != null
                ? room.getEndsAt().toInstant(ZoneOffset.UTC)
                : null;

        return new SharedResultDTO(displayMode(summary.getMode()), finishedAt, standings);
    }

    private boolean isRanked(String mode) {
        return "DUEL".equals(mode) || "TEAM".equals(mode) || "RANKED_ARENA".equals(mode);
    }

    private String displayMode(String mode) {
        if (mode == null) return "MATCH";
        return switch (mode) {
            case "RANKED_ARENA" -> "RANKED ARENA";
            case "DUEL" -> "DUEL";
            case "TEAM" -> "TEAM BATTLE";
            case "BLITZ" -> "BLITZ";
            case "PRACTICE" -> "PRACTICE";
            case "DAILY" -> "DAILY CHALLENGE";
            default -> mode;
        };
    }

    private String buildShareUrl(String token) {
        return baseUrl + "/battle/share/" + token;
    }

    private String generateUniqueToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
            for (int i = 0; i < TOKEN_LENGTH; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            String token = sb.toString();
            if (!sharedResultRepository.existsById(token)) {
                return token;
            }
        }
        throw new IllegalStateException("Failed to generate unique share token after 10 attempts");
    }
}
