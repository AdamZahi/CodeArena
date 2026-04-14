package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.*;
import com.codearena.module9_arenatalk.repository.HubMemberRepository;
import com.codearena.module9_arenatalk.repository.HubRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HubMemberService {

    private final HubMemberRepository hubMemberRepository;
    private final HubRepository hubRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public HubMember joinHub(Long hubId, String keycloakId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hub not found"));

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (hubMemberRepository.existsByHubAndUser(hub, user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already a member or request pending");
        }

        HubMember member;

        if (hub.getVisibility() == HubVisibility.PUBLIC) {
            member = HubMember.builder()
                    .hub(hub)
                    .user(user)
                    .role(MemberRole.MEMBER)
                    .status(MemberStatus.ACTIVE)
                    .online(false)
                    .lastSeen(LocalDateTime.now())
                    .build();
        } else {
            member = HubMember.builder()
                    .hub(hub)
                    .user(user)
                    .role(MemberRole.PENDING)
                    .status(MemberStatus.PENDING)
                    .online(false)
                    .lastSeen(LocalDateTime.now())
                    .build();
        }

        return hubMemberRepository.save(member);
    }

    public void leaveHub(Long hubId, String keycloakId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hub not found"));

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        HubMember member = hubMemberRepository.findByHubAndUser(hub, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member"));

        if (member.getRole() == MemberRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner cannot leave the hub");
        }

        hubMemberRepository.delete(member);
    }

    public HubMember acceptRequest(Long hubId, Long memberId, String keycloakId) {
        verifyOwner(hubId, keycloakId);

        HubMember member = hubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (member.getStatus() != MemberStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not pending");
        }

        member.setRole(MemberRole.MEMBER);
        member.setStatus(MemberStatus.ACTIVE);
        member.setOnline(false);
        member.setLastSeen(LocalDateTime.now());

        HubMember saved = hubMemberRepository.save(member);

        notificationService.createNotification(
                member.getUser(),
                member.getHub().getName(),
                member.getHub().getId(),
                NotificationType.ACCEPTED
        );

        return saved;
    }

    public void rejectRequest(Long hubId, Long memberId, String keycloakId) {
        verifyOwner(hubId, keycloakId);

        HubMember member = hubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (member.getStatus() != MemberStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not pending");
        }

        notificationService.createNotification(
                member.getUser(),
                member.getHub().getName(),
                member.getHub().getId(),
                NotificationType.REJECTED
        );

        hubMemberRepository.delete(member);
    }

    public List<HubMember> getActiveMembers(Long hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hub not found"));
        return hubMemberRepository.findByHubAndStatus(hub, MemberStatus.ACTIVE);
    }

    public List<HubMember> getPendingRequests(Long hubId, String keycloakId) {
        verifyOwner(hubId, keycloakId);
        return hubMemberRepository.findByHubIdAndStatus(hubId, MemberStatus.PENDING);
    }

    public HubMember setOnline(Long hubId, String keycloakId) {
        HubMember member = hubMemberRepository.findByHubIdAndUserKeycloakId(hubId, keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        member.setOnline(true);
        member.setLastSeen(LocalDateTime.now());
        return hubMemberRepository.save(member);
    }

    public HubMember setOffline(Long hubId, String keycloakId) {
        HubMember member = hubMemberRepository.findByHubIdAndUserKeycloakId(hubId, keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        member.setOnline(false);
        member.setLastSeen(LocalDateTime.now());
        return hubMemberRepository.save(member);
    }

    private void verifyOwner(Long hubId, String keycloakId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hub not found"));

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        HubMember caller = hubMemberRepository.findByHubAndUser(hub, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));

        if (caller.getRole() != MemberRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can do this");
        }
    }

    public List<Long> getActiveHubIds(String keycloakId) {
        return hubMemberRepository.findActiveHubIdsByKeycloakId(keycloakId);
    }
}