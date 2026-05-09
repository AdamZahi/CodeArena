package com.codearena.module7_coaching.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coach_applications")
public class CoachApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String applicantName;

    private String applicantEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String cvContent;

    @Column(columnDefinition = "LONGTEXT")
    private String cvFileBase64;

    private String cvFileName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    private String adminNote;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    public enum ApplicationStatus {
        PENDING, APPROVED, REJECTED
    }
}
