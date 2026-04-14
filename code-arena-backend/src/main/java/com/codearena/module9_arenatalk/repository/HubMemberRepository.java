package com.codearena.module9_arenatalk.repository;

import com.codearena.module9_arenatalk.entity.Hub;
import com.codearena.module9_arenatalk.entity.HubMember;
import com.codearena.module9_arenatalk.entity.MemberStatus;
import com.codearena.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HubMemberRepository extends JpaRepository<HubMember, Long> {

    boolean existsByHubAndUser(Hub hub, User user);

    Optional<HubMember> findByHubAndUser(Hub hub, User user);

    List<HubMember> findByHubAndStatus(Hub hub, MemberStatus status);

    List<HubMember> findByHubIdAndStatus(Long hubId, MemberStatus status);

    @Query("SELECT hm.hub.id FROM HubMember hm WHERE hm.user.keycloakId = :keycloakId AND hm.status = 'ACTIVE'")
    List<Long> findActiveHubIdsByKeycloakId(@Param("keycloakId") String keycloakId);

    @Query("SELECT hm FROM HubMember hm WHERE hm.hub.id = :hubId AND hm.user.keycloakId = :keycloakId")
    Optional<HubMember> findByHubIdAndUserKeycloakId(@Param("hubId") Long hubId, @Param("keycloakId") String keycloakId);
}