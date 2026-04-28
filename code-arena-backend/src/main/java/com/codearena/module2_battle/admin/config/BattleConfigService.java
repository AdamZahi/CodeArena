package com.codearena.module2_battle.admin.config;

import com.codearena.module2_battle.admin.config.dto.BattleConfigDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BattleConfigService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final BattleConfigRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BattleConfigDTO get() {
        BattleConfig entity = loadOrSeed("system");
        return toDto(entity);
    }

    @Transactional
    public BattleConfigDTO update(BattleConfigDTO dto, String adminId) {
        BattleConfig entity = loadOrSeed(adminId);
        entity.setMaxParticipants(dto.maxParticipants());
        entity.setTimeLimitMinutes(dto.timeLimitMinutes());
        entity.setAllowedLanguages(serializeLanguages(dto.allowedLanguages()));
        entity.setXpRewardWinner(dto.xpRewardWinner());
        entity.setXpRewardLoser(dto.xpRewardLoser());
        entity.setMinRankRequired(dto.minRankRequired());
        entity.setAllowSpectators(Boolean.TRUE.equals(dto.allowSpectators()));
        entity.setAutoCloseAbandonedAfterMinutes(dto.autoCloseAbandonedAfterMinutes());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(adminId);
        return toDto(repository.save(entity));
    }

    /** Convenience for other admin services that need the live config. */
    @Transactional(readOnly = true)
    public BattleConfig getEntity() {
        return loadOrSeed("system");
    }

    private BattleConfig loadOrSeed(String adminId) {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            BattleConfig seeded = BattleConfig.builder()
                    .maxParticipants(2)
                    .timeLimitMinutes(30)
                    .allowedLanguages(serializeLanguages(List.of("java", "python", "cpp", "javascript", "typescript")))
                    .xpRewardWinner(100)
                    .xpRewardLoser(20)
                    .minRankRequired(null)
                    .allowSpectators(false)
                    .autoCloseAbandonedAfterMinutes(10)
                    .updatedAt(LocalDateTime.now())
                    .updatedBy(adminId == null ? "system" : adminId)
                    .build();
            return repository.save(seeded);
        });
    }

    private BattleConfigDTO toDto(BattleConfig e) {
        return new BattleConfigDTO(
                e.getMaxParticipants(),
                e.getTimeLimitMinutes(),
                deserializeLanguages(e.getAllowedLanguages()),
                e.getXpRewardWinner(),
                e.getXpRewardLoser(),
                e.getMinRankRequired(),
                Boolean.TRUE.equals(e.getAllowSpectators()),
                e.getAutoCloseAbandonedAfterMinutes(),
                e.getUpdatedAt(),
                e.getUpdatedBy()
        );
    }

    private String serializeLanguages(List<String> langs) {
        try {
            return objectMapper.writeValueAsString(langs == null ? List.of() : langs);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<String> deserializeLanguages(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(raw, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return new ArrayList<>();
        }
    }
}
