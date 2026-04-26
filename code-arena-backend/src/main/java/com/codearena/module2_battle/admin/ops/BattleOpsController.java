package com.codearena.module2_battle.admin.ops;

import com.codearena.module2_battle.admin.management.dto.BattleRoomAdminDTO;
import com.codearena.module2_battle.admin.ops.dto.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/battles/ops")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BattleOpsController {

    private final BattleOpsService service;

    @PostMapping("/rooms/{id}/force-end")
    public ResponseEntity<BattleRoomAdminDTO> forceEnd(@PathVariable("id") UUID id,
                                                       @Valid @RequestBody ForceEndRequestDTO request,
                                                       JwtAuthenticationToken principal) {
        return ResponseEntity.ok(service.forceEnd(id, request, principal.getToken().getSubject()));
    }

    @PostMapping("/rooms/{id}/reassign-winner")
    public ResponseEntity<BattleRoomAdminDTO> reassignWinner(@PathVariable("id") UUID id,
                                                             @Valid @RequestBody ReassignWinnerRequestDTO request,
                                                             JwtAuthenticationToken principal) {
        return ResponseEntity.ok(service.reassignWinner(id, request, principal.getToken().getSubject()));
    }

    @PostMapping("/rooms/{id}/reset")
    public ResponseEntity<BattleRoomAdminDTO> reset(@PathVariable("id") UUID id,
                                                    JwtAuthenticationToken principal) {
        return ResponseEntity.ok(service.reset(id, principal.getToken().getSubject()));
    }

    @GetMapping("/stuck-rooms")
    public ResponseEntity<List<StuckRoomDTO>> stuckRooms() {
        return ResponseEntity.ok(service.findStuckRooms());
    }

    @PostMapping("/bulk-cancel")
    public ResponseEntity<BulkCancelResultDTO> bulkCancel(@Valid @RequestBody BulkCancelRequestDTO request,
                                                          JwtAuthenticationToken principal) {
        return ResponseEntity.ok(service.bulkCancel(request, principal.getToken().getSubject()));
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "format", defaultValue = "csv") String format,
            HttpServletResponse response) {

        boolean json = "json".equalsIgnoreCase(format);
        String filename = "battles-" + LocalDateTime.now().toLocalDate() + (json ? ".json" : ".csv");
        response.setContentType(json ? MediaType.APPLICATION_JSON_VALUE : "text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        StreamingResponseBody body = out -> service.streamExport(from, to, format, out);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/export/estimate")
    public ResponseEntity<Map<String, Long>> estimateExport(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(Map.of("estimatedRows", service.estimateExportSize(from, to)));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLogEntryDTO>> auditLog(
            @PageableDefault(size = 25, sort = "performedAt") Pageable pageable) {
        return ResponseEntity.ok(service.getAuditLog(pageable));
    }

    @PostMapping("/rooms/{id}/send-notification")
    public ResponseEntity<Map<String, Integer>> sendNotification(@PathVariable("id") UUID id,
                                                                 @Valid @RequestBody BattleNotificationRequestDTO request,
                                                                 JwtAuthenticationToken principal) {
        int recipients = service.notifyParticipants(id, request, principal.getToken().getSubject());
        return ResponseEntity.ok(Map.of("recipients", recipients));
    }
}
