package com.codearena.module5_support.repository;

import com.codearena.module5_support.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    // TODO: Add custom query methods.
}
