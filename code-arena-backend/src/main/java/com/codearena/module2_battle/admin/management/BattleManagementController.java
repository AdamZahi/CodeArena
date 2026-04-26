package com.codearena.module2_battle.admin.management;

import com.codearena.module2_battle.admin.management.dto.BattleParticipantAdminDTO;
import com.codearena.module2_battle.admin.management.dto.BattleRoomAdminDTO;
import com.codearena.module2_battle.admin.management.dto.BattleRoomDetailDTO;
import com.codearena.module2_battle.admin.management.dto.UpdateRoomStatusRequest;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/battles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BattleManagementController {

    private final BattleManagementService service;

    @GetMapping("/rooms")
    public ResponseEntity<Page<BattleRoomAdminDTO>> listRooms(
            @RequestParam(value = "status", required = false) BattleRoomStatus status,
            @RequestParam(value = "challengeId", required = false) String challengeId,
            @RequestParam(value = "hostId", required = false) String hostId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.listRooms(status, challengeId, hostId, from, to, pageable));
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<BattleRoomDetailDTO> getRoom(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getRoom(id));
    }

    @GetMapping("/rooms/{id}/participants")
    public ResponseEntity<List<BattleParticipantAdminDTO>> listParticipants(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.listParticipants(id));
    }

    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<BattleRoomAdminDTO> updateStatus(@PathVariable("id") UUID id,
                                                           @Valid @RequestBody UpdateRoomStatusRequest req) {
        return ResponseEntity.ok(service.updateStatus(id, req.status()));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable("id") UUID id) {
        service.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
