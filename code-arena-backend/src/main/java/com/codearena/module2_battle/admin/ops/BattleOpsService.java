package com.codearena.module2_battle.admin.ops;

import com.codearena.module2_battle.admin.config.BattleConfig;
import com.codearena.module2_battle.admin.config.BattleConfigService;
import com.codearena.module2_battle.admin.management.BattleManagementService;
import com.codearena.module2_battle.admin.management.dto.BattleRoomAdminDTO;
import com.codearena.module2_battle.admin.ops.dto.*;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BattleOpsService {

    private final BattleRoomRepository roomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final BattleAuditLogRepository auditRepository;
    private final BattleConfigService configService;
    private final BattleManagementService managementService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /* =====================================================================
     * Stuck rooms
     * ===================================================================== */
    @Transactional(readOnly = true)
    public List<StuckRoomDTO> findStuckRooms() {
        BattleConfig cfg = configService.getEntity();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(cfg.getTimeLimitMinutes());
        Instant now = Instant.now();
        return roomRepository.findStuckRooms(threshold).stream()
                .map(r -> {
                    Instant startedAt = r.getStartsAt() != null
                            ? r.getStartsAt().atZone(ZoneId.systemDefault()).toInstant()
                            : r.getCreatedAt();
                    long minutesStuck = startedAt == null ? 0L
                            : Math.max(0, Duration.between(startedAt, now).toMinutes());
                    return new StuckRoomDTO(
                            r.getId().toString(),
                            r.getHostId(),
                            managementService.resolveUsername(r.getHostId()),
                            r.getMode().name(),
                            r.getCreatedAt(),
                            minutesStuck,
                            (int) participantRepository.countByRoomId(r.getId().toString())
                    );
                })
                .toList();
    }

    /* =====================================================================
     * Force end
     * ===================================================================== */
    @Transactional
    public BattleRoomAdminDTO forceEnd(UUID roomId, ForceEndRequestDTO request, String adminId) {
        BattleRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

        Map<String, Object> before = snapshotRoom(room);

        List<BattleParticipant> participants = participantRepository.findByRoomId(roomId.toString())
                .stream().filter(p -> p.getRole() == null || p.getRole() == ParticipantRole.PLAYER)
                .toList();

        // Reset existing winner ranks first to keep things consistent.
        for (BattleParticipant p : participants) {
            p.setRank(null);
        }
        if (request.winnerId() != null && !request.winnerId().isBlank()) {
            participants.stream()
                    .filter(p -> request.winnerId().equals(p.getUserId()))
                    .findFirst()
                    .ifPresent(p -> p.setRank(1));
        }
        participantRepository.saveAll(participants);

        room.setStatus(BattleRoomStatus.FINISHED);
        if (room.getEndsAt() == null) room.setEndsAt(LocalDateTime.now());
        roomRepository.save(room);

        BattleConfig cfg = configService.getEntity();
        if (request.winnerId() != null && !request.winnerId().isBlank()) {
            adjustUserXp(request.winnerId(), cfg.getXpRewardWinner());
            for (BattleParticipant p : participants) {
                if (p.getUserId() != null && !p.getUserId().equals(request.winnerId())) {
                    adjustUserXp(p.getUserId(), cfg.getXpRewardLoser());
                }
            }
        }

        Map<String, Object> after = snapshotRoom(room);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", request.reason());
        details.put("winnerId", request.winnerId());
        details.put("before", before);
        details.put("after", after);
        writeAudit(adminId, "FORCE_END", roomId.toString(), details);
        return managementService.toAdminDto(room);
    }

    /* =====================================================================
     * Reassign winner
     * ===================================================================== */
    @Transactional
    public BattleRoomAdminDTO reassignWinner(UUID roomId, ReassignWinnerRequestDTO request, String adminId) {
        BattleRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

        List<BattleParticipant> participants = participantRepository.findByRoomId(roomId.toString())
                .stream().filter(p -> p.getRole() == null || p.getRole() == ParticipantRole.PLAYER)
                .toList();

        BattleParticipant previousWinner = participants.stream()
                .filter(p -> p.getRank() != null && p.getRank() == 1)
                .findFirst().orElse(null);
        BattleParticipant newWinner = participants.stream()
                .filter(p -> request.newWinnerId().equals(p.getUserId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + request.newWinnerId() + " is not a player in this room"));

        BattleConfig cfg = configService.getEntity();

        // Reverse XP changes from previous winner (if any).
        if (previousWinner != null && previousWinner.getUserId() != null) {
            adjustUserXp(previousWinner.getUserId(), -cfg.getXpRewardWinner());
            adjustUserXp(previousWinner.getUserId(), cfg.getXpRewardLoser());
            previousWinner.setRank(null);
        }
        // Promote new winner.
        adjustUserXp(newWinner.getUserId(), -cfg.getXpRewardLoser());
        adjustUserXp(newWinner.getUserId(), cfg.getXpRewardWinner());
        newWinner.setRank(1);

        participantRepository.saveAll(participants);

        Map<String, Object> beforeDetails = new LinkedHashMap<>();
        beforeDetails.put("winnerId", previousWinner == null ? null : previousWinner.getUserId());
        Map<String, Object> afterDetails = new LinkedHashMap<>();
        afterDetails.put("winnerId", newWinner.getUserId());
        Map<String, Object> reassignDetails = new LinkedHashMap<>();
        reassignDetails.put("reason", request.reason());
        reassignDetails.put("before", beforeDetails);
        reassignDetails.put("after", afterDetails);
        writeAudit(adminId, "REASSIGN_WINNER", roomId.toString(), reassignDetails);
        return managementService.toAdminDto(room);
    }

    /* =====================================================================
     * Reset
     * ===================================================================== */
    @Transactional
    public BattleRoomAdminDTO reset(UUID roomId, String adminId) {
        BattleRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

        Map<String, Object> before = snapshotRoom(room);

        List<BattleParticipant> participants = participantRepository.findByRoomId(roomId.toString());
        BattleConfig cfg = configService.getEntity();
        for (BattleParticipant p : participants) {
            if (p.getRank() != null && p.getRank() == 1 && p.getUserId() != null) {
                adjustUserXp(p.getUserId(), -cfg.getXpRewardWinner());
            } else if (p.getUserId() != null && (p.getRole() == null || p.getRole() == ParticipantRole.PLAYER)) {
                adjustUserXp(p.getUserId(), -cfg.getXpRewardLoser());
            }
            p.setRank(null);
            p.setScore(null);
            p.setEloChange(null);
            p.setIsReady(false);
        }
        participantRepository.saveAll(participants);

        room.setStatus(BattleRoomStatus.WAITING);
        room.setStartsAt(null);
        room.setEndsAt(null);
        roomRepository.save(room);

        writeAudit(adminId, "RESET", roomId.toString(), Map.of(
                "before", before,
                "after", snapshotRoom(room)
        ));
        return managementService.toAdminDto(room);
    }

    /* =====================================================================
     * Bulk cancel
     * ===================================================================== */
    @Transactional
    public BulkCancelResultDTO bulkCancel(BulkCancelRequestDTO request, String adminId) {
        List<String> notFound = new ArrayList<>();
        int cancelled = 0;
        for (String idStr : request.roomIds()) {
            UUID id;
            try {
                id = UUID.fromString(idStr);
            } catch (IllegalArgumentException ex) {
                notFound.add(idStr);
                continue;
            }
            BattleRoom room = roomRepository.findById(id).orElse(null);
            if (room == null) {
                notFound.add(idStr);
                continue;
            }
            room.setStatus(BattleRoomStatus.CANCELLED);
            if (room.getEndsAt() == null) room.setEndsAt(LocalDateTime.now());
            roomRepository.save(room);
            cancelled++;
        }
        writeAudit(adminId, "BULK_CANCEL", null, Map.of(
                "reason", request.reason(),
                "requested", request.roomIds(),
                "cancelled", cancelled,
                "notFound", notFound
        ));
        return new BulkCancelResultDTO(request.roomIds().size(), cancelled, notFound);
    }

    /* =====================================================================
     * Export
     * ===================================================================== */
    public void streamExport(LocalDateTime from, LocalDateTime to, String format, OutputStream out) {
        Instant fromI = (from == null ? LocalDateTime.now().minusDays(30) : from)
                .atZone(ZoneId.systemDefault()).toInstant();
        Instant toI = (to == null ? LocalDateTime.now() : to)
                .atZone(ZoneId.systemDefault()).toInstant();

        List<BattleRoom> rooms = roomRepository.findInRange(fromI, toI);

        boolean json = "json".equalsIgnoreCase(format);
        try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            if (json) {
                w.write("[");
                for (int i = 0; i < rooms.size(); i++) {
                    BattleRoom r = rooms.get(i);
                    Map<String, Object> row = exportRow(r);
                    w.write(objectMapper.writeValueAsString(row));
                    if (i < rooms.size() - 1) w.write(",");
                }
                w.write("]");
            } else {
                w.write("id,mode,status,host_id,participants,winner_id,starts_at,ends_at,created_at\n");
                for (BattleRoom r : rooms) {
                    List<BattleParticipant> participants = participantRepository.findByRoomId(r.getId().toString());
                    String winnerId = participants.stream()
                            .filter(p -> p.getRank() != null && p.getRank() == 1)
                            .map(BattleParticipant::getUserId)
                            .findFirst().orElse("");
                    w.write(String.join(",",
                            csv(r.getId().toString()),
                            csv(r.getMode().name()),
                            csv(r.getStatus().name()),
                            csv(r.getHostId()),
                            String.valueOf(participants.size()),
                            csv(winnerId),
                            csv(r.getStartsAt() == null ? "" : r.getStartsAt().toString()),
                            csv(r.getEndsAt() == null ? "" : r.getEndsAt().toString()),
                            csv(r.getCreatedAt() == null ? "" : r.getCreatedAt().toString())
                    ));
                    w.write("\n");
                }
            }
            w.flush();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write export", ex);
        }
    }

    /** Conservative row count estimate for the export UI. */
    @Transactional(readOnly = true)
    public long estimateExportSize(LocalDateTime from, LocalDateTime to) {
        Instant fromI = (from == null ? LocalDateTime.now().minusDays(30) : from)
                .atZone(ZoneId.systemDefault()).toInstant();
        Instant toI = (to == null ? LocalDateTime.now() : to)
                .atZone(ZoneId.systemDefault()).toInstant();
        return roomRepository.countByCreatedAtBetween(fromI, toI);
    }

    /* =====================================================================
     * Audit log
     * ===================================================================== */
    @Transactional(readOnly = true)
    public Page<AuditLogEntryDTO> getAuditLog(Pageable pageable) {
        return auditRepository.findAllOrderByPerformedAtDesc(pageable).map(this::toAuditDto);
    }

    /* =====================================================================
     * Notify participants
     * ===================================================================== */
    @Transactional
    public int notifyParticipants(UUID roomId, BattleNotificationRequestDTO request, String adminId) {
        BattleRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

        List<BattleParticipant> participants = participantRepository.findByRoomId(roomId.toString());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ADMIN_NOTIFICATION");
        payload.put("title", request.title());
        payload.put("message", request.message());
        payload.put("roomId", room.getId().toString());
        payload.put("sentAt", Instant.now().toString());

        // Broadcast to the room lobby topic + each user's personal queue.
        messagingTemplate.convertAndSend("/topic/battle/lobby/" + room.getId(), payload);
        for (BattleParticipant p : participants) {
            if (p.getUserId() != null) {
                messagingTemplate.convertAndSendToUser(p.getUserId(), "/queue/notifications", payload);
            }
        }

        writeAudit(adminId, "NOTIFY", roomId.toString(), Map.of(
                "title", request.title(),
                "message", request.message(),
                "recipients", participants.size()
        ));
        return participants.size();
    }

    /* =====================================================================
     * Internal helpers
     * ===================================================================== */
    private void writeAudit(String adminId, String action, String roomId, Map<String, ?> details) {
        String json;
        try {
            json = objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            json = "{\"_serializationError\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
        }
        auditRepository.save(BattleAuditLog.builder()
                .adminId(adminId)
                .action(action)
                .targetRoomId(roomId)
                .details(json)
                .performedAt(LocalDateTime.now())
                .build());
    }

    private void adjustUserXp(String auth0Id, int delta) {
        if (auth0Id == null || delta == 0) return;
        userRepository.findByAuth0Id(auth0Id).ifPresent(u -> {
            long current = u.getTotalXp() == null ? 0L : u.getTotalXp();
            long next = Math.max(0L, current + delta);
            u.setTotalXp(next);
            u.setLevel((int) (next / 500) + 1);
            userRepository.save(u);
        });
    }

    private Map<String, Object> snapshotRoom(BattleRoom r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", r.getStatus().name());
        m.put("startsAt", r.getStartsAt() == null ? null : r.getStartsAt().toString());
        m.put("endsAt", r.getEndsAt() == null ? null : r.getEndsAt().toString());
        return m;
    }

    private Map<String, Object> exportRow(BattleRoom r) {
        List<BattleParticipant> participants = participantRepository.findByRoomId(r.getId().toString());
        String winnerId = participants.stream()
                .filter(p -> p.getRank() != null && p.getRank() == 1)
                .map(BattleParticipant::getUserId)
                .findFirst().orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId().toString());
        m.put("mode", r.getMode().name());
        m.put("status", r.getStatus().name());
        m.put("hostId", r.getHostId());
        m.put("participants", participants.size());
        m.put("winnerId", winnerId);
        m.put("startsAt", r.getStartsAt());
        m.put("endsAt", r.getEndsAt());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private AuditLogEntryDTO toAuditDto(BattleAuditLog a) {
        String username = null;
        if (a.getAdminId() != null) {
            User u = userRepository.findByAuth0Id(a.getAdminId()).orElse(null);
            if (u != null) {
                username = u.getNickname() != null ? u.getNickname()
                        : (u.getEmail() != null ? u.getEmail() : a.getAdminId());
            }
        }
        return new AuditLogEntryDTO(
                a.getId().toString(),
                a.getAdminId(),
                username,
                a.getAction(),
                a.getTargetRoomId(),
                a.getDetails(),
                a.getPerformedAt()
        );
    }
}
