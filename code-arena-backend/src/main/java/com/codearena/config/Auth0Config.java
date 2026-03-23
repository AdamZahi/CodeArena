package com.codearena.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth0")
public class Auth0Config {
    private String domain;
    private String clientId;
    private String clientSecret;
    private String audience;
    private String managementApiAudience;
}
