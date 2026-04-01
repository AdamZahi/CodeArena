package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.entity.BattleRoomChallenge;
import com.codearena.module2_battle.entity.PlayerRating;
import com.codearena.module2_battle.entity.Season;
import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.exception.*;
import com.codearena.module2_battle.repository.*;
import com.codearena.module2_battle.util.InviteTokenUtil;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleRoomService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleRoomChallengeRepository roomChallengeRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final SeasonRepository seasonRepository;
    private final BattleRoomStateMachineService stateMachineService;
    private final LobbyBroadcastService lobbyBroadcastService;
    private final InviteTokenUtil inviteTokenUtil;
    private final TaskScheduler taskScheduler;

    // ──────────────────────────────────────────────
    // 2.1 — createRoom
    // ──────────────────────────────────────────────

    @Transactional
    public RoomCreatedResponse createRoom(String hostUserId, CreateRoomRequest request) {
        BattleMode mode = parseMode(request.getMode());
        int maxPlayers = request.getMaxPlayers();
        int challengeCount = request.getChallengeCount();
        boolean isPublic = request.isPublic();

        // Mode-specific validation & overrides
        if (mode == BattleMode.DUEL && maxPlayers != 2) {
            throw new InvalidRoomConfigException("DUEL mode requires exactly 2 players");
        }
        if (mode == BattleMode.RANKED_ARENA && (maxPlayers < 8 || maxPlayers > 16)) {
            throw new InvalidRoomConfigException("RANKED_ARENA mode requires maxPlayers between 8 and 16");
        }
        if (mode == BattleMode.PRACTICE || mode == BattleMode.DAILY) {
            isPublic = false;
        }
        if (mode == BattleMode.BLITZ) {
            challengeCount = 5;
        }

        // Generate unique invite token with collision retry
        String inviteToken = generateUniqueToken();

        // Select challenges
        List<Challenge> selected = selectChallenges(request.getDifficulty(), challengeCount);

        // Persist room
        BattleRoom room = BattleRoom.builder()
                .hostId(hostUserId)
                .mode(mode)
                .maxPlayers(maxPlayers)
                .challengeCount(challengeCount)
                .inviteToken(inviteToken)
                .isPublic(isPublic)
                .status(BattleRoomStatus.WAITING)
                .build();
        room = battleRoomRepository.save(room);

        String roomIdStr = room.getId().toString();

        // Persist room challenges in shuffled order
        Collections.shuffle(selected);
        for (int i = 0; i < selected.size(); i++) {
            BattleRoomChallenge rc = BattleRoomChallenge.builder()
                    .roomId(roomIdStr)
                    .challengeId(selected.get(i).getId().toString())
                    .position(i + 1)
                    .build();
            roomChallengeRepository.save(rc);
        }

        // Auto-enroll host as first player
        BattleParticipant hostParticipant = BattleParticipant.builder()
                .roomId(roomIdStr)
                .userId(hostUserId)
                .role(ParticipantRole.PLAYER)
                .isReady(false)
                .build();
        participantRepository.save(hostParticipant);

        // Build response
        BattleRoomResponse roomResponse = buildRoomResponse(room);
        InviteLinkResponse inviteResponse = InviteLinkResponse.builder()
                .inviteToken(inviteToken)
                .inviteUrl(inviteTokenUtil.buildInviteUrl(inviteToken))
                .expiresAt(null)
                .build();

        return RoomCreatedResponse.builder()
                .room(roomResponse)
                .invite(inviteResponse)
                .build();
    }

    // ──────────────────────────────────────────────
    // 2.2 — joinRoom
    // ──────────────────────────────────────────────

    @Transactional
    public LobbyStateResponse joinRoom(String userId, JoinRoomRequest request) {
        BattleRoom room = battleRoomRepository.findByInviteToken(request.getInviteToken())
                .orElseThrow(() -> new BattleRoomNotFoundException(request.getInviteToken()));

        String roomIdStr = room.getId().toString();

        if (room.getStatus() != BattleRoomStatus.WAITING) {
            throw new RoomNotJoinableException(roomIdStr, room.getStatus().name());
        }

        participantRepository.findByRoomIdAndUserId(roomIdStr, userId)
                .ifPresent(p -> {
                    throw new DuplicateParticipantException(roomIdStr, userId);
                });

        ParticipantRole role = parseRole(request.getRole());

        if (role == ParticipantRole.PLAYER) {
            int playerCount = participantRepository.countByRoomIdAndRole(roomIdStr, ParticipantRole.PLAYER);
            if (playerCount >= room.getMaxPlayers()) {
                throw new BattleRoomFullException(roomIdStr, room.getMaxPlayers());
            }
        }

        BattleParticipant participant = BattleParticipant.builder()
                .roomId(roomIdStr)
                .userId(userId)
                .role(role)
                .isReady(false)
                .build();
        participant = participantRepository.save(participant);

        ParticipantResponse participantResponse = enrichParticipant(participant);
        lobbyBroadcastService.broadcastPlayerJoined(roomIdStr, participantResponse);

        return buildLobbyState(room, null);
    }

    // ──────────────────────────────────────────────
    // 2.3 — toggleReady
    // ──────────────────────────────────────────────

    @Transactional
    public LobbyStateResponse toggleReady(String roomId, String userId, ReadyToggleRequest request) {
        BattleParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        if (participant.getRole() != ParticipantRole.PLAYER) {
            throw new InvalidParticipantActionException("Spectators cannot toggle ready state");
        }

        BattleRoom room = loadRoom(roomId);
        if (room.getStatus() != BattleRoomStatus.WAITING) {
            throw new InvalidParticipantActionException("Cannot change ready state outside WAITING status");
        }

        participant.setIsReady(request.isReady());
        participant = participantRepository.save(participant);

        ParticipantResponse participantResponse = enrichParticipant(participant);
        lobbyBroadcastService.broadcastReadyChanged(roomId, participantResponse);

        return buildLobbyState(room, null);
    }

    // ──────────────────────────────────────────────
    // 2.4 — startBattle
    // ──────────────────────────────────────────────

    @Transactional
    public LobbyStateResponse startBattle(String roomId, String requestingUserId) {
        BattleRoom room = loadRoom(roomId);

        if (!room.getHostId().equals(requestingUserId)) {
            throw new UnauthorizedRoomActionException();
        }

        // Check all players ready — collect unready usernames for error message
        List<BattleParticipant> players = participantRepository.findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        List<String> unreadyUsernames = players.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsReady()))
                .map(p -> resolveUsername(p.getUserId()))
                .toList();
        if (!unreadyUsernames.isEmpty()) {
            throw new PlayersNotReadyException(unreadyUsernames);
        }

        if (players.size() < 2) {
            throw new InvalidRoomConfigException("At least 2 players are required to start the battle");
        }

        // Delegate transition to state machine
        BattleRoom updated = stateMachineService.transitionToCountdown(roomId, requestingUserId);

        // Schedule IN_PROGRESS transition after 5 seconds
        LocalDateTime battleStartsAt = LocalDateTime.now().plusSeconds(5);
        taskScheduler.schedule(
                () -> stateMachineService.transitionToInProgress(roomId),
                Instant.now().plusSeconds(5)
        );

        CountdownPayload countdownPayload = CountdownPayload.builder()
                .countdownSeconds(5)
                .battleStartsAt(battleStartsAt)
                .build();
        lobbyBroadcastService.broadcastCountdownStarted(roomId, countdownPayload);

        return buildLobbyState(updated, 5);
    }

    // ──────────────────────────────────────────────
    // 2.5 — kickParticipant
    // ──────────────────────────────────────────────

    @Transactional
    public LobbyStateResponse kickParticipant(String roomId, String requestingUserId, KickParticipantRequest request) {
        BattleRoom room = loadRoom(roomId);

        if (!room.getHostId().equals(requestingUserId)) {
            throw new UnauthorizedRoomActionException();
        }
        if (room.getStatus() != BattleRoomStatus.WAITING) {
            throw new InvalidParticipantActionException("Kicks are only allowed in WAITING status");
        }
        if (request.getTargetUserId().equals(requestingUserId)) {
            throw new InvalidParticipantActionException("The host cannot kick themselves");
        }

        BattleParticipant target = participantRepository.findByRoomIdAndUserId(roomId, request.getTargetUserId())
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        participantRepository.delete(target);

        lobbyBroadcastService.broadcastPlayerKicked(roomId, request.getTargetUserId());

        return buildLobbyState(room, null);
    }

    // ──────────────────────────────────────────────
    // 2.6 — leaveRoom
    // ──────────────────────────────────────────────

    @Transactional
    public void leaveRoom(String roomId, String userId) {
        BattleParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        BattleRoom room = loadRoom(roomId);

        // Host-leaves logic: reassign host or cancel room
        if (room.getHostId().equals(userId) && room.getStatus() == BattleRoomStatus.WAITING) {
            participantRepository.delete(participant);

            // Find next player by earliest joinedAt to become the new host
            List<BattleParticipant> remainingPlayers = participantRepository
                    .findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);

            if (remainingPlayers.isEmpty()) {
                stateMachineService.transitionToCancelled(roomId, "All players left");
            } else {
                // Pick the player who joined earliest as new host
                BattleParticipant newHost = remainingPlayers.stream()
                        .min(Comparator.comparing(BattleParticipant::getJoinedAt))
                        .get();
                room.setHostId(newHost.getUserId());
                battleRoomRepository.save(room);
            }
        } else if (room.getStatus() == BattleRoomStatus.COUNTDOWN
                && participant.getRole() == ParticipantRole.PLAYER) {
            // Player leaving during countdown cancels the room
            participantRepository.delete(participant);
            stateMachineService.transitionToCancelled(roomId, "Player disconnected during countdown");
        } else {
            participantRepository.delete(participant);
        }

        lobbyBroadcastService.broadcastPlayerLeft(roomId, userId);
    }

    // ──────────────────────────────────────────────
    // 2.7 — getLobbyState
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LobbyStateResponse getLobbyState(String roomId, String requestingUserId) {
        BattleRoom room = loadRoom(roomId);
        return buildLobbyState(room, null);
    }

    // ──────────────────────────────────────────────
    // 2.8 — getPublicRooms
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BattleRoomResponse> getPublicRooms() {
        List<BattleRoom> rooms = battleRoomRepository.findByStatusAndIsPublicTrue(BattleRoomStatus.WAITING);
        return rooms.stream()
                .sorted(Comparator.comparing(BattleRoom::getCreatedAt).reversed())
                .limit(20)
                .map(this::buildRoomResponse)
                .toList();
    }

    // ──────────────────────────────────────────────
    // getInviteLink
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InviteLinkResponse getInviteLink(String roomId, String requestingUserId) {
        BattleRoom room = loadRoom(roomId);
        return InviteLinkResponse.builder()
                .inviteToken(room.getInviteToken())
                .inviteUrl(inviteTokenUtil.buildInviteUrl(room.getInviteToken()))
                .expiresAt(null)
                .build();
    }

    // ──────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────

    private BattleRoom loadRoom(String roomId) {
        return battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));
    }

    private BattleMode parseMode(String mode) {
        try {
            return BattleMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoomConfigException("Invalid battle mode: " + mode);
        }
    }

    private ParticipantRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return ParticipantRole.PLAYER;
        }
        try {
            return ParticipantRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ParticipantRole.PLAYER;
        }
    }

    private String generateUniqueToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String token = inviteTokenUtil.generate();
            if (battleRoomRepository.findByInviteToken(token).isEmpty()) {
                return token;
            }
        }
        throw new IllegalStateException("Failed to generate a unique invite token after 10 attempts");
    }

    private List<Challenge> selectChallenges(String difficulty, int count) {
        List<Challenge> pool;
        if (difficulty == null || difficulty.equalsIgnoreCase("MIXED")) {
            pool = challengeRepository.findAll();
        } else {
            pool = challengeRepository.findAll().stream()
                    .filter(c -> difficulty.equalsIgnoreCase(c.getDifficulty()))
                    .collect(Collectors.toList());
        }

        if (pool.size() < count) {
            throw new NotEnoughChallengesException(
                    difficulty != null ? difficulty : "MIXED", count, pool.size());
        }

        Collections.shuffle(pool);
        return pool.subList(0, count);
    }

    private BattleRoomResponse buildRoomResponse(BattleRoom room) {
        String roomIdStr = room.getId().toString();
        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(roomIdStr, ParticipantRole.PLAYER);
        int spectatorCount = participantRepository
                .countByRoomIdAndRole(roomIdStr, ParticipantRole.SPECTATOR);

        List<ParticipantResponse> participantResponses = players.stream()
                .map(this::enrichParticipant)
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

    private LobbyStateResponse buildLobbyState(BattleRoom room, Integer countdownSeconds) {
        String roomIdStr = room.getId().toString();
        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(roomIdStr, ParticipantRole.PLAYER);
        List<BattleParticipant> spectators = participantRepository
                .findByRoomIdAndRole(roomIdStr, ParticipantRole.SPECTATOR);

        List<ParticipantResponse> playerResponses = players.stream()
                .map(this::enrichParticipant).toList();
        List<ParticipantResponse> spectatorResponses = spectators.stream()
                .map(this::enrichParticipant).toList();

        boolean allReady = players.size() >= 2
                && players.stream().allMatch(p -> Boolean.TRUE.equals(p.getIsReady()));

        BattleRoomResponse roomResponse = buildRoomResponse(room);

        return LobbyStateResponse.builder()
                .room(roomResponse)
                .players(playerResponses)
                .spectators(spectatorResponses)
                .canStart(allReady)
                .countdownSeconds(countdownSeconds != null ? countdownSeconds :
                        (room.getStatus() == BattleRoomStatus.COUNTDOWN ? 5 : 0))
                .build();
    }

    /**
     * Enriches a BattleParticipant with user profile data (username, avatar)
     * and active season rating (elo, tier).
     */
    private ParticipantResponse enrichParticipant(BattleParticipant participant) {
        String username = resolveUsername(participant.getUserId());
        String avatarUrl = null;

        User user = userRepository.findByKeycloakId(participant.getUserId()).orElse(null);
        if (user != null) {
            username = user.getNickname() != null ? user.getNickname()
                    : (user.getFirstName() != null ? user.getFirstName() : username);
            avatarUrl = user.getAvatarUrl();
        }

        // Resolve current season ELO and tier
        Integer currentElo = null;
        String tier = null;
        Optional<Season> activeSeason = seasonRepository.findByIsActiveTrue();
        if (activeSeason.isPresent()) {
            Optional<PlayerRating> rating = playerRatingRepository.findByUserIdAndSeasonId(
                    participant.getUserId(), activeSeason.get().getId().toString());
            if (rating.isPresent()) {
                currentElo = rating.get().getElo();
                tier = rating.get().getTier().name();
            }
        }

        return ParticipantResponse.builder()
                .participantId(participant.getId().toString())
                .userId(participant.getUserId())
                .username(username)
                .avatarUrl(avatarUrl)
                .role(participant.getRole().name())
                .isReady(Boolean.TRUE.equals(participant.getIsReady()))
                .currentElo(currentElo)
                .tier(tier)
                .build();
    }

    private String resolveUsername(String userId) {
        return userRepository.findByKeycloakId(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getFirstName())
                .orElse(userId);
    }
}
