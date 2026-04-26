package com.codearena.module2_battle.admin.management;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.admin.management.dto.BattleParticipantAdminDTO;
import com.codearena.module2_battle.admin.management.dto.BattleRoomAdminDTO;
import com.codearena.module2_battle.admin.management.dto.BattleRoomDetailDTO;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.entity.BattleRoomChallenge;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomChallengeRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BattleManagementService {

    private final BattleRoomRepository roomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleRoomChallengeRepository roomChallengeRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<BattleRoomAdminDTO> listRooms(BattleRoomStatus status,
                                              String challengeId,
                                              String hostId,
                                              Instant from,
                                              Instant to,
                                              Pageable pageable) {
        Specification<BattleRoom> spec = Specification.where(BattleRoomSpecifications.hasStatus(status))
                .and(BattleRoomSpecifications.hasHost(hostId))
                .and(BattleRoomSpecifications.createdBetween(from, to));

        if (challengeId != null && !challengeId.isBlank()) {
            // Resolve the rooms that contain the given challenge first, then narrow the spec.
            Set<UUID> matchingRoomIds = new HashSet<>();
            roomChallengeRepository.findAll().forEach(rc -> {
                if (challengeId.equals(rc.getChallengeId())) {
                    try {
                        matchingRoomIds.add(UUID.fromString(rc.getRoomId()));
                    } catch (IllegalArgumentException ignored) { }
                }
            });
            if (matchingRoomIds.isEmpty()) {
                return Page.empty(pageable);
            }
            spec = spec.and((root, q, cb) -> root.get("id").in(matchingRoomIds));
        }

        return roomRepository.findAll(spec, pageable).map(this::toAdminDto);
    }

    @Transactional(readOnly = true)
    public BattleRoomDetailDTO getRoom(UUID roomId) {
        BattleRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

        List<BattleParticipant> participants = participantRepository.findByRoomId(room.getId().toString());
        List<BattleRoomChallenge> challenges = roomChallengeRepository.findByRoomIdOrderByPositionAsc(room.getId().toString());

        return new BattleRoomDetailDTO(
                room.getId().toString(),
                room.getHostId(),
                resolveUsername(room.getHostId()),
                room.getMode().name(),
                room.getMaxPlayers(),
                room.getChallengeCount(),
                room.getInviteToken(),
                room.getIsPublic(),
                room.getStatus().name(),
                room.getStartsAt(),
                room.getEndsAt(),
                room.getCreatedAt(),
                challenges.stream().map(BattleRoomChallenge::getChallengeId).toList(),
                participants.stream().map(this::toParticipantDto).toList(),
                findWinnerId(participants)
        );
    }

    @Transactional(readOnly = true)
    public List<BattleParticipantAdminDTO> listParticipants(UUID roomId) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        return participantRepository.findByRoomId(roomId.toString())
                .stream()
                .map(this::toParticipantDto)
                .toList();
    }

    @Transactional
    public BattleRoomAdminDTO updateStatus(UUID roomId, BattleRoomStatus status) {
        BattleRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        room.setStatus(status);
        if (status == BattleRoomStatus.CANCELLED || status == BattleRoomStatus.FINISHED) {
            if (room.getEndsAt() == null) {
                room.setEndsAt(java.time.LocalDateTime.now());
            }
        }
        return toAdminDto(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(UUID roomId) {
        BattleRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

        // Best-effort cascading cleanup (no FK constraints in the schema).
        List<BattleParticipant> participants = participantRepository.findByRoomId(roomId.toString());
        if (!participants.isEmpty()) {
            List<String> participantIds = participants.stream().map(p -> p.getId().toString()).toList();
            var subs = submissionRepository.findByParticipantIdIn(participantIds);
            if (!subs.isEmpty()) submissionRepository.deleteAll(subs);
            participantRepository.deleteAll(participants);
        }
        roomChallengeRepository.deleteByRoomId(roomId.toString());
        roomRepository.delete(room);
    }

    // ---------- helpers (also used by Ops service) ----------

    public BattleRoomAdminDTO toAdminDto(BattleRoom room) {
        List<BattleParticipant> participants = participantRepository.findByRoomId(room.getId().toString());
        List<BattleRoomChallenge> challenges = roomChallengeRepository.findByRoomIdOrderByPositionAsc(room.getId().toString());

        String firstChallengeId = challenges.isEmpty() ? null : challenges.get(0).getChallengeId();
        String firstChallengeTitle = null;
        if (firstChallengeId != null) {
            try {
                Challenge c = challengeRepository.findById(Long.parseLong(firstChallengeId)).orElse(null);
                if (c != null) firstChallengeTitle = c.getTitle();
            } catch (NumberFormatException ignored) { }
        }

        return new BattleRoomAdminDTO(
                room.getId().toString(),
                firstChallengeId,
                firstChallengeTitle,
                room.getHostId(),
                resolveUsername(room.getHostId()),
                room.getStatus().name(),
                room.getMode().name(),
                room.getInviteToken(),
                room.getCreatedAt(),
                participants.size(),
                findWinnerId(participants)
        );
    }

    public BattleParticipantAdminDTO toParticipantDto(BattleParticipant p) {
        return new BattleParticipantAdminDTO(
                p.getId().toString(),
                p.getUserId(),
                resolveUsername(p.getUserId()),
                p.getRole() == null ? ParticipantRole.PLAYER.name() : p.getRole().name(),
                p.getIsReady(),
                p.getScore(),
                p.getRank(),
                p.getEloChange(),
                p.getJoinedAt()
        );
    }

    public String resolveUsername(String userId) {
        if (userId == null || userId.isBlank()) return null;
        User user = userRepository.findByAuth0Id(userId).orElse(null);
        if (user == null) return userId;
        if (user.getNickname() != null && !user.getNickname().isBlank()) return user.getNickname();
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null) sb.append(user.getFirstName());
        if (user.getLastName() != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(user.getLastName());
        }
        if (sb.length() > 0) return sb.toString();
        if (user.getEmail() != null) return user.getEmail();
        return userId;
    }

    private String findWinnerId(List<BattleParticipant> participants) {
        return participants.stream()
                .filter(p -> p.getRank() != null && p.getRank() == 1)
                .filter(p -> p.getRole() == null || p.getRole() == ParticipantRole.PLAYER)
                .map(BattleParticipant::getUserId)
                .findFirst()
                .orElse(null);
    }
}
