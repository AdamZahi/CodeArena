package com.codearena.module2_battle.admin.ops;

import com.codearena.module2_battle.admin.config.BattleConfig;
import com.codearena.module2_battle.admin.config.BattleConfigService;
import com.codearena.module2_battle.admin.management.BattleManagementService;
import com.codearena.module2_battle.admin.management.dto.BattleRoomAdminDTO;
import com.codearena.module2_battle.admin.ops.dto.*;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BattleOpsServiceTest {

    @Mock private BattleRoomRepository roomRepository;
    @Mock private BattleParticipantRepository participantRepository;
    @Mock private BattleSubmissionRepository submissionRepository;
    @Mock private BattleAuditLogRepository auditRepository;
    @Mock private BattleConfigService configService;
    @Mock private BattleManagementService managementService;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BattleOpsService service;

    @BeforeEach
    void setUp() {
        service = new BattleOpsService(
                roomRepository, participantRepository, submissionRepository,
                auditRepository, configService, managementService,
                userRepository, objectMapper, messagingTemplate);
    }

    private BattleRoom room(UUID id, BattleRoomStatus status) {
        return BattleRoom.builder()
                .id(id).hostId("auth0|host").mode(BattleMode.DUEL)
                .status(status).maxPlayers(2).challengeCount(1).isPublic(true)
                .createdAt(Instant.now()).build();
    }

    private BattleParticipant player(String userId, Integer rank) {
        return BattleParticipant.builder()
                .id(UUID.randomUUID()).roomId(UUID.randomUUID().toString())
                .userId(userId).role(ParticipantRole.PLAYER).rank(rank).build();
    }

    @Test
    void forceEnd_promotesWinnerAwardsXpAndAudits() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.IN_PROGRESS);
        BattleParticipant alice = player("auth0|alice", null);
        BattleParticipant bob = player("auth0|bob", null);
        alice.setRoomId(id.toString());
        bob.setRoomId(id.toString());

        when(roomRepository.findById(id)).thenReturn(Optional.of(r));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of(alice, bob));
        when(configService.getEntity()).thenReturn(BattleConfig.builder()
                .xpRewardWinner(100).xpRewardLoser(20).timeLimitMinutes(30).build());
        when(roomRepository.save(any(BattleRoom.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByAuth0Id("auth0|alice"))
                .thenReturn(Optional.of(User.builder().auth0Id("auth0|alice").totalXp(0L).build()));
        when(userRepository.findByAuth0Id("auth0|bob"))
                .thenReturn(Optional.of(User.builder().auth0Id("auth0|bob").totalXp(0L).build()));
        when(managementService.toAdminDto(any(BattleRoom.class))).thenReturn(
                new BattleRoomAdminDTO(id.toString(), null, null, "auth0|host", "host",
                        "FINISHED", "DUEL", null, r.getCreatedAt(), 2, "auth0|alice"));

        service.forceEnd(id, new ForceEndRequestDTO("auth0|alice", "stuck game"), "auth0|admin");

        assertThat(r.getStatus()).isEqualTo(BattleRoomStatus.FINISHED);
        assertThat(r.getEndsAt()).isNotNull();
        assertThat(alice.getRank()).isEqualTo(1);
        assertThat(bob.getRank()).isNull();

        ArgumentCaptor<BattleAuditLog> audit = ArgumentCaptor.forClass(BattleAuditLog.class);
        verify(auditRepository).save(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("FORCE_END");
        assertThat(audit.getValue().getDetails()).contains("auth0|alice").contains("stuck game");
    }

        @Test
        void forceEnd_allowsDrawWithoutWinner() {
                UUID id = UUID.randomUUID();
                BattleRoom r = room(id, BattleRoomStatus.IN_PROGRESS);
                BattleParticipant alice = player("auth0|alice", null);
                alice.setRoomId(id.toString());

                when(roomRepository.findById(id)).thenReturn(Optional.of(r));
                when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of(alice));
                when(configService.getEntity()).thenReturn(BattleConfig.builder()
                                .xpRewardWinner(100).xpRewardLoser(20).timeLimitMinutes(30).build());
                when(roomRepository.save(any(BattleRoom.class))).thenAnswer(inv -> inv.getArgument(0));
                when(managementService.toAdminDto(any(BattleRoom.class))).thenReturn(
                                new BattleRoomAdminDTO(id.toString(), null, null, "auth0|host", "host",
                                                "FINISHED", "DUEL", null, r.getCreatedAt(), 1, null));

                service.forceEnd(id, new ForceEndRequestDTO(null, "draw by admin"), "auth0|admin");

                assertThat(r.getStatus()).isEqualTo(BattleRoomStatus.FINISHED);
                assertThat(alice.getRank()).isNull();
                verify(auditRepository).save(any(BattleAuditLog.class));
        }

    @Test
    void reassignWinner_swapsRanksAndAdjustsXp() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.FINISHED);
        BattleParticipant alice = player("auth0|alice", 1);
        BattleParticipant bob = player("auth0|bob", null);
        alice.setRoomId(id.toString());
        bob.setRoomId(id.toString());

        when(roomRepository.findById(id)).thenReturn(Optional.of(r));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of(alice, bob));
        when(configService.getEntity()).thenReturn(BattleConfig.builder()
                .xpRewardWinner(100).xpRewardLoser(20).build());
        when(userRepository.findByAuth0Id("auth0|alice"))
                .thenReturn(Optional.of(User.builder().auth0Id("auth0|alice").totalXp(100L).build()));
        when(userRepository.findByAuth0Id("auth0|bob"))
                .thenReturn(Optional.of(User.builder().auth0Id("auth0|bob").totalXp(20L).build()));
        when(managementService.toAdminDto(any(BattleRoom.class))).thenReturn(
                new BattleRoomAdminDTO(id.toString(), null, null, null, null,
                        "FINISHED", "DUEL", null, r.getCreatedAt(), 2, "auth0|bob"));

        service.reassignWinner(id, new ReassignWinnerRequestDTO("auth0|bob", "scoring error"), "auth0|admin");

        assertThat(alice.getRank()).isNull();
        assertThat(bob.getRank()).isEqualTo(1);

        ArgumentCaptor<BattleAuditLog> audit = ArgumentCaptor.forClass(BattleAuditLog.class);
        verify(auditRepository).save(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("REASSIGN_WINNER");
    }

    @Test
    void reset_resetsParticipantsAndStatusToWaiting() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.FINISHED);
        r.setStartsAt(LocalDateTime.now().minusMinutes(10));
        r.setEndsAt(LocalDateTime.now());
        BattleParticipant alice = player("auth0|alice", 1);
        alice.setScore(80);
        alice.setRoomId(id.toString());

        when(roomRepository.findById(id)).thenReturn(Optional.of(r));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of(alice));
        when(configService.getEntity()).thenReturn(BattleConfig.builder()
                .xpRewardWinner(100).xpRewardLoser(20).build());
        when(userRepository.findByAuth0Id("auth0|alice"))
                .thenReturn(Optional.of(User.builder().auth0Id("auth0|alice").totalXp(100L).build()));
        when(roomRepository.save(any(BattleRoom.class))).thenAnswer(inv -> inv.getArgument(0));
        when(managementService.toAdminDto(any(BattleRoom.class))).thenReturn(
                new BattleRoomAdminDTO(id.toString(), null, null, null, null,
                        "WAITING", "DUEL", null, r.getCreatedAt(), 1, null));

        service.reset(id, "auth0|admin");

        assertThat(r.getStatus()).isEqualTo(BattleRoomStatus.WAITING);
        assertThat(r.getStartsAt()).isNull();
        assertThat(r.getEndsAt()).isNull();
        assertThat(alice.getRank()).isNull();
        assertThat(alice.getScore()).isNull();
        verify(auditRepository).save(any(BattleAuditLog.class));
    }

    @Test
    void bulkCancel_collectsNotFoundIds() {
        UUID known = UUID.randomUUID();
        BattleRoom r = room(known, BattleRoomStatus.IN_PROGRESS);
        when(roomRepository.findById(known)).thenReturn(Optional.of(r));
        when(roomRepository.save(any(BattleRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkCancelResultDTO result = service.bulkCancel(
                new BulkCancelRequestDTO(List.of(known.toString(), "not-a-uuid"), "broken"),
                "auth0|admin");

        assertThat(result.requested()).isEqualTo(2);
        assertThat(result.cancelled()).isEqualTo(1);
        assertThat(result.notFound()).contains("not-a-uuid");
        assertThat(r.getStatus()).isEqualTo(BattleRoomStatus.CANCELLED);
        verify(auditRepository).save(any(BattleAuditLog.class));
    }

    @Test
    void notifyParticipants_broadcastsToTopicAndUserQueue() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.IN_PROGRESS);
        BattleParticipant alice = player("auth0|alice", null);
        alice.setRoomId(id.toString());

        when(roomRepository.findById(id)).thenReturn(Optional.of(r));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of(alice));

        int recipients = service.notifyParticipants(id,
                new BattleNotificationRequestDTO("Heads up", "Maintenance soon"),
                "auth0|admin");

        assertThat(recipients).isEqualTo(1);
        verify(messagingTemplate).convertAndSend(eq("/topic/battle/lobby/" + id), any(Object.class));
        verify(messagingTemplate).convertAndSendToUser(eq("auth0|alice"), eq("/queue/notifications"), any(Object.class));
        verify(auditRepository).save(any(BattleAuditLog.class));
    }

    @Test
    void streamExport_writesCsvHeaderAndRow() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.FINISHED);
        when(roomRepository.findInRange(any(), any())).thenReturn(List.of(r));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamExport(LocalDateTime.now().minusDays(1), LocalDateTime.now(), "csv", out);

        String csv = out.toString();
        assertThat(csv).startsWith("id,mode,status,host_id,participants,winner_id,starts_at,ends_at,created_at\n");
        assertThat(csv).contains(id.toString()).contains("DUEL").contains("FINISHED");
    }
}
