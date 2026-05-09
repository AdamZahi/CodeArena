package com.codearena.module9_arenatalk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Community name is required")
    @Size(min = 3, max = 30, message = "Community name must be between 3 and 30 characters")
    private String name;

    @Column(length = 1000)
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 200, message = "Description must be between 10 and 200 characters")
    private String description;

    private String bannerUrl;

    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Category is required")
    private HubCategory category;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Visibility is required")
    private HubVisibility visibility;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "hub", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TextChannel> textChannels;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}