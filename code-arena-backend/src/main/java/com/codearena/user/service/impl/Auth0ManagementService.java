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
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class Auth0ManagementService {
    private final Auth0Config auth0Config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicBoolean missingConfigLogged = new AtomicBoolean(false);

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

        try {
            String url = String.format("https://%s/api/v2/users/%s/roles", auth0Config.getDomain(), auth0UserId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of("roles", Collections.singletonList(roleId));
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(payload, headers), Void.class);
        } catch (Exception e) {
            log.warn("Auth0 role sync failed for user {}", auth0UserId);
            log.debug("Auth0 role sync exception", e);
        }
    }

    public Auth0UserProfile fetchUserProfile(String auth0UserId) {
        String token = getManagementToken();
        if (token == null || auth0UserId == null || auth0UserId.isBlank()) {
            return null;
        }

        try {
            String encodedUserId = URLEncoder.encode(auth0UserId, StandardCharsets.UTF_8);
            String url = String.format("https://%s/api/v2/users/%s", auth0Config.getDomain(), encodedUserId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (response.getBody() == null) {
                return null;
            }

            Map body = response.getBody();
            return Auth0UserProfile.builder()
                    .email(asString(body.get("email")))
                    .name(asString(body.get("name")))
                    .nickname(asString(body.get("nickname")))
                    .givenName(asString(body.get("given_name")))
                    .familyName(asString(body.get("family_name")))
                    .picture(asString(body.get("picture")))
                    .build();
        } catch (Exception e) {
            log.warn("Could not fetch Auth0 profile for {}: {}", auth0UserId, e.getMessage());
            log.debug("Auth0 profile fetch exception", e);
            return null;
        }
    }

    public Auth0UserProfile getUserProfile(String auth0UserId) {
        return fetchUserProfile(auth0UserId);
    }

    private String resolveRoleId(String token, String roleName) {
        try {
            String url = String.format("https://%s/api/v2/roles", auth0Config.getDomain());
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<Object[]> response =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object[].class);
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
        } catch (Exception e) {
            log.debug("Auth0 role lookup failed for role {}", roleName, e);
        }
        return null;
    }

    private String getManagementToken() {
        if (!hasManagementConfig()) {
            return null;
        }

        try {
            String tokenUrl = String.format("https://%s/oauth/token", auth0Config.getDomain());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", auth0Config.getClientId());
            form.add("client_secret", auth0Config.getClientSecret());
            form.add("audience", auth0Config.getManagementApiAudience());
            form.add("grant_type", "client_credentials");

            ResponseEntity<Map> response =
                    restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
            if (response.getBody() == null) {
                return null;
            }
            Object token = response.getBody().get("access_token");
            return token == null ? null : token.toString();
        } catch (Exception e) {
            log.warn("Failed to obtain Auth0 management token: {}", e.getMessage());
            log.debug("Auth0 management token exception", e);
            return null;
        }
    }

    private boolean hasManagementConfig() {
        boolean valid = hasText(auth0Config.getDomain())
                && hasText(auth0Config.getClientId())
                && hasText(auth0Config.getClientSecret())
                && hasText(auth0Config.getManagementApiAudience());
        if (!valid && missingConfigLogged.compareAndSet(false, true)) {
            log.warn("Auth0 management config is missing/incomplete. Name hydration from Auth0 is disabled.");
        }
        return valid;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    @lombok.Builder
    @lombok.Value
    public static class Auth0UserProfile {
        String email;
        String name;
        String nickname;
        String givenName;
        String familyName;
        String picture;
    }
}
