package com.codearena.module2_battle.admin.management;

import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.admin.management.dto.BattleParticipantAdminDTO;
import com.codearena.module2_battle.admin.management.dto.BattleRoomAdminDTO;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.entity.BattleRoomChallenge;
import com.codearena.module2_battle.entity.BattleSubmission;
import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomChallengeRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import com.codearena.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BattleManagementServiceTest {

    @Mock private BattleRoomRepository roomRepository;
    @Mock private BattleParticipantRepository participantRepository;
    @Mock private BattleRoomChallengeRepository roomChallengeRepository;
    @Mock private BattleSubmissionRepository submissionRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private BattleManagementService service;

    private BattleRoom room(UUID id, BattleRoomStatus status) {
        return BattleRoom.builder()
                .id(id)
                .hostId("auth0|host")
                .mode(BattleMode.DUEL)
                .maxPlayers(2)
                .challengeCount(1)
                .isPublic(true)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getRoom_returnsDetailWithParticipantsAndWinner() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.FINISHED);
        BattleParticipant winner = BattleParticipant.builder()
                .id(UUID.randomUUID()).roomId(id.toString())
                .userId("auth0|alice").role(ParticipantRole.PLAYER).rank(1).build();

        when(roomRepository.findById(id)).thenReturn(Optional.of(r));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of(winner));
        when(roomChallengeRepository.findByRoomIdOrderByPositionAsc(id.toString()))
                .thenReturn(List.of(BattleRoomChallenge.builder().challengeId("99").position(1).build()));
        when(userRepository.findByAuth0Id(any())).thenReturn(Optional.empty());

        var detail = service.getRoom(id);

        assertThat(detail.id()).isEqualTo(id.toString());
        assertThat(detail.winnerId()).isEqualTo("auth0|alice");
        assertThat(detail.participants()).hasSize(1);
        assertThat(detail.challengeIds()).containsExactly("99");
    }

    @Test
    void getRoom_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(roomRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRoom(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateStatus_setsEndsAtForTerminalStates() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.IN_PROGRESS);
        when(roomRepository.findById(id)).thenReturn(Optional.of(r));
        when(roomRepository.save(any(BattleRoom.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of());
        when(roomChallengeRepository.findByRoomIdOrderByPositionAsc(id.toString())).thenReturn(List.of());

        BattleRoomAdminDTO dto = service.updateStatus(id, BattleRoomStatus.CANCELLED);

        assertThat(dto.status()).isEqualTo("CANCELLED");
        assertThat(r.getEndsAt()).isNotNull();
    }

    @Test
    void deleteRoom_cascadesParticipantsSubmissionsAndChallenges() {
        UUID id = UUID.randomUUID();
        BattleRoom r = room(id, BattleRoomStatus.FINISHED);
        BattleParticipant p = BattleParticipant.builder()
                .id(UUID.randomUUID()).roomId(id.toString()).build();
        BattleSubmission s = BattleSubmission.builder()
                .participantId(p.getId().toString())
                .roomChallengeId("rc-1").language("java").code("class A{}").build();

        when(roomRepository.findById(id)).thenReturn(Optional.of(r));
        when(participantRepository.findByRoomId(id.toString())).thenReturn(List.of(p));
        when(submissionRepository.findByParticipantIdIn(List.of(p.getId().toString())))
                .thenReturn(List.of(s));

        service.deleteRoom(id);

        verify(submissionRepository).deleteAll(List.of(s));
        verify(participantRepository).deleteAll(List.of(p));
        verify(roomChallengeRepository).deleteByRoomId(id.toString());
        verify(roomRepository).delete(r);
    }
}
