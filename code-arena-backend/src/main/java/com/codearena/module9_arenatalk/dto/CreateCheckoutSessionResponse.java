package com.codearena.module9_arenatalk.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateCheckoutSessionResponse {
    private String checkoutUrl;
}