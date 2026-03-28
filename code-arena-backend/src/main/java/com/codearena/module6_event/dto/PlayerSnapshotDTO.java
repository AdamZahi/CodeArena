package com.codearena.module6_event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerSnapshotDTO {

    @JsonProperty("userId")
    private String userId;

    private String tier;
    private Integer wins;

    @JsonProperty("eloRating")
    private Integer eloRating;
}

