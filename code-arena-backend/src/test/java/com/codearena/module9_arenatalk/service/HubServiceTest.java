package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.CreateHubRequestDTO;
import com.codearena.module9_arenatalk.entity.*;
import com.codearena.module9_arenatalk.repository.HubMemberRepository;
import com.codearena.module9_arenatalk.repository.HubRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HubServiceTest {

    @Mock private HubRepository       hubRepository;
    @Mock private HubMemberRepository hubMemberRepository;
    @Mock private UserRepository      userRepository;

    @InjectMocks
    private HubService hubService;

    private Hub               hub;
    private User              owner;
    private CreateHubRequestDTO dto;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setKeycloakId("owner-keycloak-id");
        owner.setFirstName("Owner");
        owner.setLastName("Test");

        hub = new Hub();
        hub.setId(1L);
        hub.setName("Test Hub");
        hub.setDescription("A test hub");
        hub.setCategory(HubCategory.GAMING);
        hub.setVisibility(HubVisibility.PUBLIC);

        dto = new CreateHubRequestDTO();
        dto.setName("Test Hub");
        dto.setDescription("A test hub");
        dto.setCategory(HubCategory.GAMING);
        dto.setVisibility(HubVisibility.PUBLIC);
    }

    // ── createHub ─────────────────────────────────────────────────────────────

    @Test
    void createHub_shouldSaveHub_whenNameIsUnique() {
        when(hubRepository.existsByNameIgnoreCase("Test Hub")).thenReturn(false);
        when(hubRepository.save(any(Hub.class))).thenReturn(hub);
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.save(any(HubMember.class))).thenAnswer(inv -> inv.getArgument(0));

        Hub result = hubService.createHub(dto, "owner-keycloak-id");

        assertNotNull(result);
        assertEquals("Test Hub", result.getName());
        verify(hubRepository, times(1)).save(any(Hub.class));
    }

    @Test
    void createHub_shouldThrowException_whenNameAlreadyExists() {
        when(hubRepository.existsByNameIgnoreCase("Test Hub")).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                hubService.createHub(dto, "owner-keycloak-id")
        );
        verify(hubRepository, never()).save(any());
    }

    @Test
    void createHub_shouldCreateOwnerMember_afterSavingHub() {
        when(hubRepository.existsByNameIgnoreCase("Test Hub")).thenReturn(false);
        when(hubRepository.save(any(Hub.class))).thenReturn(hub);
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.save(any(HubMember.class))).thenAnswer(inv -> inv.getArgument(0));

        hubService.createHub(dto, "owner-keycloak-id");

        verify(hubMemberRepository, times(1)).save(argThat(m ->
                m.getRole() == MemberRole.OWNER &&
                        m.getStatus() == MemberStatus.ACTIVE
        ));
    }

    @Test
    void createHub_shouldThrowException_whenUserNotFound() {
        when(hubRepository.existsByNameIgnoreCase("Test Hub")).thenReturn(false);
        when(hubRepository.save(any(Hub.class))).thenReturn(hub);
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                hubService.createHub(dto, "unknown")
        );
    }

    @Test
    void createHub_shouldTrimHubName() {
        dto.setName("  Test Hub  ");
        when(hubRepository.existsByNameIgnoreCase("Test Hub")).thenReturn(false);
        when(hubRepository.save(any(Hub.class))).thenAnswer(inv -> {
            Hub h = inv.getArgument(0);
            assertEquals("Test Hub", h.getName());
            return hub;
        });
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.save(any())).thenReturn(new HubMember());

        hubService.createHub(dto, "owner-keycloak-id");
        verify(hubRepository, times(1)).save(any());
    }

    // ── getAllHubs ────────────────────────────────────────────────────────────

    @Test
    void getAllHubs_shouldReturnAllHubs() {
        when(hubRepository.findAll()).thenReturn(List.of(hub));

        List<Hub> result = hubService.getAllHubs();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllHubs_shouldReturnEmptyList_whenNoHubs() {
        when(hubRepository.findAll()).thenReturn(List.of());

        List<Hub> result = hubService.getAllHubs();

        assertTrue(result.isEmpty());
    }

    // ── getHubById ────────────────────────────────────────────────────────────

    @Test
    void getHubById_shouldReturnHub_whenExists() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));

        Hub result = hubService.getHubById(1L);

        assertNotNull(result);
        assertEquals("Test Hub", result.getName());
    }

    @Test
    void getHubById_shouldThrowException_whenNotFound() {
        when(hubRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                hubService.getHubById(99L)
        );
    }

    // ── deleteHub ─────────────────────────────────────────────────────────────

    @Test
    void deleteHub_shouldDeleteHub_whenExists() {
        when(hubRepository.existsById(1L)).thenReturn(true);
        doNothing().when(hubRepository).deleteById(1L);

        hubService.deleteHub(1L);

        verify(hubRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteHub_shouldThrowException_whenNotFound() {
        when(hubRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                hubService.deleteHub(99L)
        );
        verify(hubRepository, never()).deleteById(any());
    }

    // ── updateHub ─────────────────────────────────────────────────────────────

    @Test
    void updateHub_shouldUpdateHub_whenExists() {
        CreateHubRequestDTO updateDto = new CreateHubRequestDTO();
        updateDto.setName("Updated Hub");
        updateDto.setDescription("Updated description");
        updateDto.setCategory(HubCategory.GAMING);
        updateDto.setVisibility(HubVisibility.PUBLIC);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(hubRepository.existsByNameIgnoreCaseAndIdNot("Updated Hub", 1L)).thenReturn(false);
        when(hubRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Hub result = hubService.updateHub(1L, updateDto);

        assertEquals("Updated Hub", result.getName());
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    void updateHub_shouldThrowException_whenHubNotFound() {
        when(hubRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                hubService.updateHub(99L, dto)
        );
    }

    @Test
    void updateHub_shouldThrowException_whenNameTakenByAnotherHub() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(hub));
        when(hubRepository.existsByNameIgnoreCaseAndIdNot("Test Hub", 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                hubService.updateHub(1L, dto)
        );
    }
}