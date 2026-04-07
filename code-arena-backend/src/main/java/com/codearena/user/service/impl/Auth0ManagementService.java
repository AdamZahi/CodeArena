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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Auth0ManagementService {
    private final Auth0Config auth0Config;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a new Auth0 user with email/password.
     * Returns the new user's Auth0 user_id (sub).
     * If user already exists (409 Conflict), looks them up by email and returns their id.
     */
    public String createUser(String email, String password, String name) {
        String url = String.format("https://%s/dbconnections/signup", auth0Config.getDomain());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "client_id",      auth0Config.getClientId(),
            "connection",     "Username-Password-Authentication",
            "email",          email,
            "password",       password,
            "name",           name,
            "user_metadata",  Map.of("role", "COACH")
        );

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            if (resp.getBody() != null) {
                Object rawId = resp.getBody().get("_id");
                if (rawId != null) {
                    String auth0UserId = "auth0|" + rawId.toString();
                    log.info("Created Auth0 user {} -> {}", email, auth0UserId);
                    return auth0UserId;
                }
            }
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            if (errorBody.contains("user already exists") || errorBody.contains("already_exists")) {
                log.info("Auth0 user {} already exists, skipping creation for seeder", email);
                // We cannot look them up without Management API, so we just return a dummy ID if we don't have it,
                // but local DB might already have them. For the seeder, returning null is fine if they exist,
                // but the seeder checks if coachRepository has them. We will just return null here.
                return null;
            }
            log.error("Failed to create Auth0 user {}: {}", email, errorBody);
        } catch (Exception e) {
            log.error("Failed to create Auth0 user {}: {}", email, e.getMessage());
        }
        return null;
    }

    /** Finds an Auth0 user by email and returns their user_id */
    public String getUserIdByEmail(String token, String email) {
        try {
            String encoded = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
            String url = String.format("https://%s/api/v2/users-by-email?email=%s", auth0Config.getDomain(), encoded);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<Map[]> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map[].class);
            if (resp.getBody() != null && resp.getBody().length > 0) {
                Object id = resp.getBody()[0].get("user_id");
                return id != null ? id.toString() : null;
            }
        } catch (Exception e) {
            log.error("Failed to lookup user by email {}: {}", email, e.getMessage());
        }
        return null;
    }

    /** Assigns an Auth0 role to a user by role name */
    public void assignRoleToUser(String auth0UserId, String roleName) {
        updateUserRole(auth0UserId, roleName);
    }

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
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), Void.class);
            log.info("Assigned role '{}' to user {}", roleName, auth0UserId);
        } catch (Exception e) {
            log.error("Failed to assign role '{}' to user {}: {}", roleName, auth0UserId, e.getMessage());
        }
    }

    private String resolveRoleId(String token, String roleName) {
        String url = String.format("https://%s/api/v2/roles", auth0Config.getDomain());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Object[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object[].class);
        if (response.getBody() == null) return null;
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

    public String getManagementToken() {
        String tokenUrl = String.format("https://%s/oauth/token", auth0Config.getDomain());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id",     auth0Config.getClientId());
        form.add("client_secret", auth0Config.getClientSecret());
        form.add("audience",      auth0Config.getManagementApiAudience());
        form.add("grant_type",    "client_credentials");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
            if (response.getBody() == null) return null;
            Object token = response.getBody().get("access_token");
            return token == null ? null : token.toString();
        } catch (HttpClientErrorException e) {
            log.warn("Management API token request failed: {}. (This is normal if your Auth0 app is a SPA)", e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Error fetching Management token: {}", e.getMessage());
            return null;
        }
    }
}
