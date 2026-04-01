package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.exception.UserNotFoundException;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchHistoryService {

    private final UserRepository userRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final BattleProfileService battleProfileService;

    public MatchHistoryPageResponse getMatchHistory(
            String targetUserId, String requestingUserId, MatchHistoryPageRequest request) {

        userRepository.findByKeycloakId(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        int size = Math.min(Math.max(request.getSize(), 1), 50);
        int page = Math.max(request.getPage(), 0);

        // Load all finished participants for this user
        List<BattleParticipant> allFinished = participantRepository.findByUserId(targetUserId).stream()
                .filter(p -> p.getRole() == ParticipantRole.PLAYER)
                .filter(p -> {
                    Optional<BattleRoom> roomOpt = battleRoomRepository.findById(UUID.fromString(p.getRoomId()));
                    return roomOpt.isPresent() && roomOpt.get().getStatus() == BattleRoomStatus.FINISHED;
                })
                .toList();

        // Apply mode filter
        List<BattleParticipant> filtered = allFinished;
        if (request.getMode() != null && !request.getMode().isBlank()) {
            BattleMode modeFilter = BattleMode.valueOf(request.getMode().toUpperCase());
            filtered = filtered.stream().filter(p -> {
                BattleRoom room = battleRoomRepository.findById(UUID.fromString(p.getRoomId())).orElse(null);
                return room != null && room.getMode() == modeFilter;
            }).toList();
        }

        // Apply result filter: WIN = rank 1, LOSS = rank == totalPlayers, DRAW = middle
        if (request.getResult() != null && !request.getResult().isBlank()) {
            String resultFilter = request.getResult().toUpperCase();
            filtered = filtered.stream().filter(p -> {
                int totalPlayers = participantRepository.countByRoomIdAndRole(p.getRoomId(), ParticipantRole.PLAYER);
                return switch (resultFilter) {
                    case "WIN" -> p.getRank() != null && p.getRank() == 1;
                    case "LOSS" -> p.getRank() != null && p.getRank() == totalPlayers;
                    case "DRAW" -> p.getRank() != null && p.getRank() != 1 && p.getRank() != totalPlayers;
                    default -> true;
                };
            }).toList();
        }

        // Sort by room.endsAt DESC
        filtered = filtered.stream().sorted((a, b) -> {
            BattleRoom roomA = battleRoomRepository.findById(UUID.fromString(a.getRoomId())).orElse(null);
            BattleRoom roomB = battleRoomRepository.findById(UUID.fromString(b.getRoomId())).orElse(null);
            if (roomA == null || roomB == null) return 0;
            if (roomA.getEndsAt() == null || roomB.getEndsAt() == null) return 0;
            return roomB.getEndsAt().compareTo(roomA.getEndsAt());
        }).collect(Collectors.toList());

        int totalMatches = filtered.size();

        // Apply pagination
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<BattleParticipant> pageContent = fromIndex < filtered.size()
                ? filtered.subList(fromIndex, toIndex)
                : List.of();

        List<MatchHistorySummaryResponse> matches = pageContent.stream()
                .map(battleProfileService::buildMatchSummary)
                .filter(Objects::nonNull)
                .toList();

        return MatchHistoryPageResponse.builder()
                .userId(targetUserId)
                .totalMatches(totalMatches)
                .page(page)
                .size(size)
                .matches(matches)
                .build();
    }
}
