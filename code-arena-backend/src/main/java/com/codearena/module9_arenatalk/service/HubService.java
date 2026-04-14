package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.dto.CreateHubRequestDTO;
import com.codearena.module9_arenatalk.entity.*;
import com.codearena.module9_arenatalk.repository.HubMemberRepository;
import com.codearena.module9_arenatalk.repository.HubRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;
    private final HubMemberRepository hubMemberRepository;
    private final UserRepository userRepository;

    // keycloakId vient du JWT dans le controller
    public Hub createHub(CreateHubRequestDTO dto, String keycloakId) {
        if (hubRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new RuntimeException("A community with this name already exists.");
        }

        // 1. Créer le hub
        Hub hub = Hub.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription().trim())
                .bannerUrl(dto.getBannerUrl() != null ? dto.getBannerUrl().trim() : null)
                .iconUrl(dto.getIconUrl() != null ? dto.getIconUrl().trim() : null)
                .category(dto.getCategory())
                .visibility(dto.getVisibility())
                .build();

        Hub savedHub = hubRepository.save(hub);

        // 2. Récupérer le user depuis keycloakId
        User owner = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 3. Créer le HubMember OWNER automatiquement
        HubMember ownerMember = HubMember.builder()
                .hub(savedHub)
                .user(owner)
                .role(MemberRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .build();

        hubMemberRepository.save(ownerMember);

        return savedHub;
    }

    public List<Hub> getAllHubs() {
        return hubRepository.findAll();
    }

    public Hub getHubById(Long id) {
        return hubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hub not found with id: " + id));
    }

    public void deleteHub(Long id) {
        if (!hubRepository.existsById(id)) {
            throw new RuntimeException("Hub not found with id: " + id);
        }
        hubRepository.deleteById(id);
    }

    public Hub updateHub(Long id, CreateHubRequestDTO dto) {
        Hub existingHub = hubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hub not found with id: " + id));

        if (hubRepository.existsByNameIgnoreCaseAndIdNot(dto.getName().trim(), id)) {
            throw new RuntimeException("Another community with this name already exists.");
        }

        existingHub.setName(dto.getName().trim());
        existingHub.setDescription(dto.getDescription().trim());
        existingHub.setBannerUrl(dto.getBannerUrl() != null ? dto.getBannerUrl().trim() : null);
        existingHub.setIconUrl(dto.getIconUrl() != null ? dto.getIconUrl().trim() : null);
        existingHub.setCategory(dto.getCategory());
        existingHub.setVisibility(dto.getVisibility());

        return hubRepository.save(existingHub);
    }
}