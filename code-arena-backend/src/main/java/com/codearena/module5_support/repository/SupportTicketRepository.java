package com.codearena.module5_support.repository;

import com.codearena.module5_support.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    // TODO: Add custom query methods.
}
