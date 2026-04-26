package com.codearena.module2_battle.admin.config;

import com.codearena.module2_battle.admin.config.dto.BattleConfigDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/battles/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BattleConfigController {

    private final BattleConfigService service;

    @GetMapping
    public ResponseEntity<BattleConfigDTO> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    public ResponseEntity<BattleConfigDTO> update(@Valid @RequestBody BattleConfigDTO dto,
                                                  JwtAuthenticationToken principal) {
        String adminId = principal.getToken().getSubject();
        return ResponseEntity.ok(service.update(dto, adminId));
    }
}
