package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.BattleFeedItemResponse;
import com.codearena.module2_battle.dto.BattleFeedResponse;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BattleFeedService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public BattleFeedResponse getFeed() {
        // Live rooms: IN_PROGRESS, public, ordered by starts_at DESC, cap at 10
        List<BattleRoom> liveRooms = battleRoomRepository
                .findByStatusAndIsPublicTrueOrderByStartsAtDesc(
                        BattleRoomStatus.IN_PROGRESS, PageRequest.of(0, 10));
        long totalLiveCount = battleRoomRepository
                .countByStatusAndIsPublicTrue(BattleRoomStatus.IN_PROGRESS);

        // Open rooms: WAITING, public, ordered by created_at DESC, cap at 10
        List<BattleRoom> openRooms = battleRoomRepository
                .findByStatusAndIsPublicTrueOrderByCreatedAtDesc(
                        BattleRoomStatus.WAITING, PageRequest.of(0, 10));
        long totalOpenCount = battleRoomRepository
                .countByStatusAndIsPublicTrue(BattleRoomStatus.WAITING);

        // Recent rooms: FINISHED, public, finished within last 2 hours, cap at 5
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        List<BattleRoom> recentRooms = battleRoomRepository
                .findRecentFinishedPublic(BattleRoomStatus.FINISHED, twoHoursAgo, PageRequest.of(0, 5));

        return BattleFeedResponse.builder()
                .live(liveRooms.stream().map(this::toFeedItem).toList())
                .open(openRooms.stream().map(this::toFeedItem).toList())
                .recent(recentRooms.stream().map(this::toFeedItem).toList())
                .totalLiveCount(totalLiveCount)
                .totalOpenCount(totalOpenCount)
                .build();
    }

    private BattleFeedItemResponse toFeedItem(BattleRoom room) {
        String roomId = room.getId().toString();

        int playerCount = participantRepository.countByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        int spectatorCount = participantRepository.countByRoomIdAndRole(roomId, ParticipantRole.SPECTATOR);

        // Host username
        String hostUsername = resolveUsername(room.getHostId());

        // Up to 4 player usernames
        List<BattleParticipant> players = participantRepository.findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        List<String> playerUsernames = new ArrayList<>();
        for (int i = 0; i < Math.min(4, players.size()); i++) {
            playerUsernames.add(resolveUsername(players.get(i).getUserId()));
        }
        if (players.size() > 4) {
            playerUsernames.add("+ " + (players.size() - 4) + " more");
        }

        // Seconds ago
        long secondsAgo = 0;
        if (room.getCreatedAt() != null) {
            secondsAgo = Duration.between(room.getCreatedAt(), Instant.now()).getSeconds();
        }

        // Mode badge — human-readable mode label
        String modeBadge = buildModeBadge(room.getMode());

        return BattleFeedItemResponse.builder()
                .roomId(roomId)
                .mode(room.getMode().name())
                .status(room.getStatus().name())
                .isLive(room.getStatus() == BattleRoomStatus.IN_PROGRESS)
                .playerCount(playerCount)
                .maxPlayers(room.getMaxPlayers())
                .spectatorCount(spectatorCount)
                .hostUsername(hostUsername)
                .playerUsernames(playerUsernames)
                .createdAt(room.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(room.getCreatedAt(), java.time.ZoneOffset.UTC)
                        : null)
                .secondsAgo(secondsAgo)
                .modeBadge(modeBadge)
                .build();
    }

    /**
     * Builds a human-readable mode label for the feed display.
     */
    private String buildModeBadge(BattleMode mode) {
        return switch (mode) {
            case DUEL -> "1v1 Duel";
            case TEAM -> "Team Battle";
            case RANKED_ARENA -> "Ranked Arena";
            case BLITZ -> "Blitz";
            case PRACTICE -> "Practice";
            case DAILY -> "Daily Challenge";
        };
    }

    private String resolveUsername(String userId) {
        if (userId == null) return "Unknown";
        return userRepository.findByKeycloakId(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getFirstName())
                .orElse(userId);
    }
}
