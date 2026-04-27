package com.codearena.module9_arenatalk.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCheckoutSessionRequest {
    private String userId;
    private String userName;
    private Integer coins;
}