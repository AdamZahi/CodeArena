package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.dto.CreateHubRequestDTO;
import com.codearena.module9_arenatalk.entity.Hub;
import com.codearena.module9_arenatalk.service.HubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/arenatalk/hubs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class HubController {

    private final HubService hubService;

    @PostMapping
    public Hub createHub(@Valid @RequestBody CreateHubRequestDTO dto) {
        String keycloakId = dto.getKeycloakId();
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keycloakId is required");
        }
        return hubService.createHub(dto, keycloakId);
    }

    @GetMapping
    public List<Hub> getAllHubs() {
        return hubService.getAllHubs();
    }

    @GetMapping("/{id}")
    public Hub getHubById(@PathVariable Long id) {
        return hubService.getHubById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteHub(@PathVariable Long id) {
        hubService.deleteHub(id);
    }

    @PutMapping("/{id}")
    public Hub updateHub(@PathVariable Long id, @Valid @RequestBody CreateHubRequestDTO dto) {
        return hubService.updateHub(id, dto);
    }
}