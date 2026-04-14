package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.dto.JoinHubRequestDTO;
import com.codearena.module9_arenatalk.entity.HubMember;
import com.codearena.module9_arenatalk.service.HubMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/arenatalk/hubs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class HubMemberController {

    private final HubMemberService hubMemberService;

    @PostMapping("/{hubId}/join")
    public ResponseEntity<HubMember> joinHub(@PathVariable Long hubId,
                                             @RequestBody JoinHubRequestDTO dto) {
        return ResponseEntity.ok(hubMemberService.joinHub(hubId, dto.getKeycloakId()));
    }

    @DeleteMapping("/{hubId}/leave")
    public ResponseEntity<Void> leaveHub(@PathVariable Long hubId,
                                         @RequestBody JoinHubRequestDTO dto) {
        hubMemberService.leaveHub(hubId, dto.getKeycloakId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{hubId}/members")
    public ResponseEntity<List<HubMember>> getMembers(@PathVariable Long hubId) {
        return ResponseEntity.ok(hubMemberService.getActiveMembers(hubId));
    }

    @GetMapping("/{hubId}/requests")
    public ResponseEntity<List<HubMember>> getPendingRequests(@PathVariable Long hubId,
                                                              @RequestParam String keycloakId) {
        return ResponseEntity.ok(hubMemberService.getPendingRequests(hubId, keycloakId));
    }

    @PostMapping("/{hubId}/requests/{memberId}/accept")
    public ResponseEntity<HubMember> acceptRequest(@PathVariable Long hubId,
                                                   @PathVariable Long memberId,
                                                   @RequestBody JoinHubRequestDTO dto) {
        return ResponseEntity.ok(hubMemberService.acceptRequest(hubId, memberId, dto.getKeycloakId()));
    }

    @DeleteMapping("/{hubId}/requests/{memberId}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long hubId,
                                              @PathVariable Long memberId,
                                              @RequestBody JoinHubRequestDTO dto) {
        hubMemberService.rejectRequest(hubId, memberId, dto.getKeycloakId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-hubs")
    public ResponseEntity<List<Long>> getMyHubIds(@RequestParam String keycloakId) {
        return ResponseEntity.ok(hubMemberService.getActiveHubIds(keycloakId));
    }

    @PutMapping("/{hubId}/online")
    public ResponseEntity<HubMember> setOnline(@PathVariable Long hubId,
                                               @RequestParam String keycloakId) {
        return ResponseEntity.ok(hubMemberService.setOnline(hubId, keycloakId));
    }

    @PutMapping("/{hubId}/offline")
    public ResponseEntity<HubMember> setOffline(@PathVariable Long hubId,
                                                @RequestParam String keycloakId) {
        return ResponseEntity.ok(hubMemberService.setOffline(hubId, keycloakId));
    }
}