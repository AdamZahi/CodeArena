package com.codearena.module2_battle.admin.config;

import com.codearena.module2_battle.admin.config.dto.BattleConfigDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BattleConfigServiceTest {

    @Mock private BattleConfigRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BattleConfigService service;

    @BeforeEach
    void setUp() {
        service = new BattleConfigService(repository, objectMapper);
    }

    @Test
    void get_seedsConfigWhenMissing() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any(BattleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        BattleConfigDTO dto = service.get();

        assertThat(dto.maxParticipants()).isEqualTo(2);
        assertThat(dto.allowedLanguages()).contains("java", "python");
        verify(repository).save(any(BattleConfig.class));
    }

    @Test
    void update_writesAdminAndTimestamp() {
        BattleConfig existing = BattleConfig.builder()
                .id(1).maxParticipants(2).timeLimitMinutes(30)
                .allowedLanguages("[\"java\"]")
                .xpRewardWinner(100).xpRewardLoser(20)
                .allowSpectators(false)
                .autoCloseAbandonedAfterMinutes(10)
                .updatedAt(java.time.LocalDateTime.now()).updatedBy("system")
                .build();
        when(repository.findAll()).thenReturn(List.of(existing));
        when(repository.save(any(BattleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        BattleConfigDTO incoming = new BattleConfigDTO(
                4, 45, List.of("java", "rust"),
                150, 30, "GOLD", true, 15, null, null);

        BattleConfigDTO result = service.update(incoming, "auth0|admin");

        ArgumentCaptor<BattleConfig> captor = ArgumentCaptor.forClass(BattleConfig.class);
        verify(repository).save(captor.capture());
        BattleConfig saved = captor.getValue();
        assertThat(saved.getMaxParticipants()).isEqualTo(4);
        assertThat(saved.getAllowedLanguages()).contains("rust");
        assertThat(saved.getUpdatedBy()).isEqualTo("auth0|admin");
        assertThat(saved.getAllowSpectators()).isTrue();
        assertThat(result.minRankRequired()).isEqualTo("GOLD");
    }

    @Test
    void getEntity_returnsExistingWithoutSeeding() {
        BattleConfig existing = BattleConfig.builder().id(1).maxParticipants(8).build();
        when(repository.findAll()).thenReturn(List.of(existing));
        BattleConfig result = service.getEntity();
        assertThat(result.getMaxParticipants()).isEqualTo(8);
        verify(repository, never()).save(any());
    }
}
