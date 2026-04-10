package com.codearena.user.service.impl;

import com.codearena.config.Auth0Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Auth0ManagementService {
    private final Auth0Config auth0Config;
    private final RestTemplate restTemplate = new RestTemplate();

    public record Auth0UserProfile(
        String email,
        String givenName,
        String familyName,
        String name,
        String nickname
    ) {}

    public void updateUserRole(String auth0UserId, String roleName) {
        String token = getManagementToken();
        if (token == null) {
            log.warn("Auth0 management token unavailable, role sync skipped");
            return;
        }

        String roleId = resolveRoleId(token, roleName);
        if (roleId == null) {
            log.warn("Auth0 role id not found for role {}", roleName);
            return;
        }

        String url = String.format("https://%s/api/v2/users/%s/roles", auth0Config.getDomain(), auth0UserId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of("roles", Collections.singletonList(roleId));
        restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(payload, headers), Void.class);
    }

    public Auth0UserProfile getUserProfile(String auth0UserId) {
        String token = getManagementToken();
        if (token == null || auth0UserId == null || auth0UserId.isBlank()) {
            return null;
        }

        try {
            String encodedUserId = URLEncoder.encode(auth0UserId, StandardCharsets.UTF_8);
            String url = String.format("https://%s/api/v2/users/%s", auth0Config.getDomain(), encodedUserId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) {
                return null;
            }

            return new Auth0UserProfile(
                asString(body.get("email")),
                asString(body.get("given_name")),
                asString(body.get("family_name")),
                asString(body.get("name")),
                asString(body.get("nickname"))
            );
        } catch (Exception ex) {
            log.warn("Failed to load Auth0 profile for {}: {}", auth0UserId, ex.getMessage());
            return null;
        }
    }

    private String resolveRoleId(String token, String roleName) {
        String url = String.format("https://%s/api/v2/roles", auth0Config.getDomain());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Object[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object[].class);
        if (response.getBody() == null) {
            return null;
        }
        for (Object role : response.getBody()) {
            if (role instanceof Map<?, ?> map) {
                Object name = map.get("name");
                Object id = map.get("id");
                if (name != null && id != null && roleName.equalsIgnoreCase(name.toString())) {
                    return id.toString();
                }
            }
        }
        return null;
    }

    private String getManagementToken() {
        String tokenUrl = String.format("https://%s/oauth/token", auth0Config.getDomain());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", auth0Config.getClientId());
        form.add("client_secret", auth0Config.getClientSecret());
        form.add("audience", auth0Config.getManagementApiAudience());
        form.add("grant_type", "client_credentials");

        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
        if (response.getBody() == null) {
            return null;
        }
        Object token = response.getBody().get("access_token");
        return token == null ? null : token.toString();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
