package com.codearena.module9_arenatalk.service;

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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HubMemberServiceTest {

    @Mock private HubMemberRepository   hubMemberRepository;
    @Mock private HubRepository         hubRepository;
    @Mock private UserRepository        userRepository;
    @Mock private NotificationService   notificationService;
    @Mock private ArenatalkEmailService arenatalkEmailService;

    @InjectMocks
    private HubMemberService hubMemberService;

    private Hub  publicHub;
    private Hub  privateHub;
    private User user;
    private User owner;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("user-keycloak-id");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setKeycloakId("owner-keycloak-id");
        owner.setFirstName("Owner");
        owner.setLastName("Test");
        owner.setEmail("owner@test.com");

        publicHub = new Hub();
        publicHub.setId(1L);
        publicHub.setName("Public Hub");
        publicHub.setVisibility(HubVisibility.PUBLIC);

        privateHub = new Hub();
        privateHub.setId(2L);
        privateHub.setName("Private Hub");
        privateHub.setVisibility(HubVisibility.PRIVATE);
    }

    // ── joinHub ───────────────────────────────────────────────────────────────

    @Test
    void joinHub_shouldJoinAsActive_whenHubIsPublic() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(hubMemberRepository.existsByHubAndUser(publicHub, user)).thenReturn(false);
        when(hubMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HubMember result = hubMemberService.joinHub(1L, "user-keycloak-id");

        assertEquals(MemberStatus.ACTIVE, result.getStatus());
        assertEquals(MemberRole.MEMBER, result.getRole());
    }

    @Test
    void joinHub_shouldJoinAsPending_whenHubIsPrivate() {
        when(hubRepository.findById(2L)).thenReturn(Optional.of(privateHub));
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(hubMemberRepository.existsByHubAndUser(privateHub, user)).thenReturn(false);
        when(hubMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HubMember result = hubMemberService.joinHub(2L, "user-keycloak-id");

        assertEquals(MemberStatus.PENDING, result.getStatus());
        assertEquals(MemberRole.PENDING, result.getRole());
    }

    @Test
    void joinHub_shouldThrowConflict_whenAlreadyMember() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(hubMemberRepository.existsByHubAndUser(publicHub, user)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () ->
                hubMemberService.joinHub(1L, "user-keycloak-id")
        );
    }

    @Test
    void joinHub_shouldThrowNotFound_whenHubDoesNotExist() {
        when(hubRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                hubMemberService.joinHub(99L, "user-keycloak-id")
        );
    }

    @Test
    void joinHub_shouldThrowNotFound_whenUserDoesNotExist() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                hubMemberService.joinHub(1L, "unknown")
        );
    }

    // ── leaveHub ──────────────────────────────────────────────────────────────

    @Test
    void leaveHub_shouldDeleteMember_whenNotOwner() {
        HubMember member = new HubMember();
        member.setRole(MemberRole.MEMBER);
        member.setStatus(MemberStatus.ACTIVE);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("user-keycloak-id")).thenReturn(Optional.of(user));
        when(hubMemberRepository.findByHubAndUser(publicHub, user)).thenReturn(Optional.of(member));

        hubMemberService.leaveHub(1L, "user-keycloak-id");

        verify(hubMemberRepository, times(1)).delete(member);
    }

    @Test
    void leaveHub_shouldThrowForbidden_whenOwnerTriesToLeave() {
        HubMember member = new HubMember();
        member.setRole(MemberRole.OWNER);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(member));

        assertThrows(ResponseStatusException.class, () ->
                hubMemberService.leaveHub(1L, "owner-keycloak-id")
        );

        verify(hubMemberRepository, never()).delete(any());
    }

    // ── acceptRequest ─────────────────────────────────────────────────────────

    @Test
    void acceptRequest_shouldSetStatusToActive() {
        HubMember ownerMember = new HubMember();
        ownerMember.setRole(MemberRole.OWNER);

        HubMember pendingMember = new HubMember();
        pendingMember.setStatus(MemberStatus.PENDING);
        pendingMember.setUser(user);
        pendingMember.setHub(publicHub);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(ownerMember));
        when(hubMemberRepository.findById(10L)).thenReturn(Optional.of(pendingMember));
        when(hubMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HubMember result = hubMemberService.acceptRequest(1L, 10L, "owner-keycloak-id");

        assertEquals(MemberStatus.ACTIVE, result.getStatus());
        assertEquals(MemberRole.MEMBER, result.getRole());
    }

    @Test
    void acceptRequest_shouldSendAcceptanceEmail() {
        HubMember ownerMember = new HubMember();
        ownerMember.setRole(MemberRole.OWNER);

        HubMember pendingMember = new HubMember();
        pendingMember.setStatus(MemberStatus.PENDING);
        pendingMember.setUser(user);
        pendingMember.setHub(publicHub);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(ownerMember));
        when(hubMemberRepository.findById(10L)).thenReturn(Optional.of(pendingMember));
        when(hubMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hubMemberService.acceptRequest(1L, 10L, "owner-keycloak-id");

        verify(arenatalkEmailService, times(1))
                .sendHubAcceptedEmail(eq("john@test.com"), any(), eq("Public Hub"));
    }

    @Test
    void acceptRequest_shouldThrowBadRequest_whenMemberIsNotPending() {
        HubMember ownerMember = new HubMember();
        ownerMember.setRole(MemberRole.OWNER);

        HubMember activeMember = new HubMember();
        activeMember.setStatus(MemberStatus.ACTIVE);
        activeMember.setUser(user);
        activeMember.setHub(publicHub);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(ownerMember));
        when(hubMemberRepository.findById(10L)).thenReturn(Optional.of(activeMember));

        assertThrows(ResponseStatusException.class, () ->
                hubMemberService.acceptRequest(1L, 10L, "owner-keycloak-id")
        );
    }

    @Test
    void acceptRequest_shouldCreateNotification() {
        HubMember ownerMember = new HubMember();
        ownerMember.setRole(MemberRole.OWNER);

        HubMember pendingMember = new HubMember();
        pendingMember.setStatus(MemberStatus.PENDING);
        pendingMember.setUser(user);
        pendingMember.setHub(publicHub);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(ownerMember));
        when(hubMemberRepository.findById(10L)).thenReturn(Optional.of(pendingMember));
        when(hubMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hubMemberService.acceptRequest(1L, 10L, "owner-keycloak-id");

        verify(notificationService, times(1))
                .createNotification(eq(user), eq("Public Hub"), eq(1L), eq(NotificationType.ACCEPTED));
    }

    // ── rejectRequest ─────────────────────────────────────────────────────────

    @Test
    void rejectRequest_shouldDeleteMember() {
        HubMember ownerMember = new HubMember();
        ownerMember.setRole(MemberRole.OWNER);

        HubMember pendingMember = new HubMember();
        pendingMember.setStatus(MemberStatus.PENDING);
        pendingMember.setUser(user);
        pendingMember.setHub(publicHub);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(ownerMember));
        when(hubMemberRepository.findById(10L)).thenReturn(Optional.of(pendingMember));

        hubMemberService.rejectRequest(1L, 10L, "owner-keycloak-id");

        verify(hubMemberRepository, times(1)).delete(pendingMember);
    }

    @Test
    void rejectRequest_shouldSendRejectionEmail() {
        HubMember ownerMember = new HubMember();
        ownerMember.setRole(MemberRole.OWNER);

        HubMember pendingMember = new HubMember();
        pendingMember.setStatus(MemberStatus.PENDING);
        pendingMember.setUser(user);
        pendingMember.setHub(publicHub);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(ownerMember));
        when(hubMemberRepository.findById(10L)).thenReturn(Optional.of(pendingMember));

        hubMemberService.rejectRequest(1L, 10L, "owner-keycloak-id");

        verify(arenatalkEmailService, times(1))
                .sendHubRejectedEmail(eq("john@test.com"), any(), eq("Public Hub"));
    }

    @Test
    void rejectRequest_shouldCreateRejectionNotification() {
        HubMember ownerMember = new HubMember();
        ownerMember.setRole(MemberRole.OWNER);

        HubMember pendingMember = new HubMember();
        pendingMember.setStatus(MemberStatus.PENDING);
        pendingMember.setUser(user);
        pendingMember.setHub(publicHub);

        when(hubRepository.findById(1L)).thenReturn(Optional.of(publicHub));
        when(userRepository.findByKeycloakId("owner-keycloak-id")).thenReturn(Optional.of(owner));
        when(hubMemberRepository.findByHubAndUser(publicHub, owner)).thenReturn(Optional.of(ownerMember));
        when(hubMemberRepository.findById(10L)).thenReturn(Optional.of(pendingMember));

        hubMemberService.rejectRequest(1L, 10L, "owner-keycloak-id");

        verify(notificationService, times(1))
                .createNotification(eq(user), eq("Public Hub"), eq(1L), eq(NotificationType.REJECTED));
    }

    // ── setOnline / setOffline ────────────────────────────────────────────────

    @Test
    void setOnline_shouldSetOnlineToTrue() {
        HubMember member = new HubMember();
        member.setOnline(false);

        when(hubMemberRepository.findByHubIdAndUserKeycloakId(1L, "user-keycloak-id"))
                .thenReturn(Optional.of(member));
        when(hubMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HubMember result = hubMemberService.setOnline(1L, "user-keycloak-id");

        assertTrue(result.isOnline());
    }

    @Test
    void setOffline_shouldSetOnlineToFalse() {
        HubMember member = new HubMember();
        member.setOnline(true);

        when(hubMemberRepository.findByHubIdAndUserKeycloakId(1L, "user-keycloak-id"))
                .thenReturn(Optional.of(member));
        when(hubMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HubMember result = hubMemberService.setOffline(1L, "user-keycloak-id");

        assertFalse(result.isOnline());
    }

    @Test
    void setOnline_shouldThrowNotFound_whenMemberDoesNotExist() {
        when(hubMemberRepository.findByHubIdAndUserKeycloakId(1L, "unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                hubMemberService.setOnline(1L, "unknown")
        );
    }
}