package com.codearena.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    /**
     * Configures security filter chain.
     *
     * @param http HttpSecurity
     * @return configured filter chain
     * @throws Exception when configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        
                        // ─── PUBLIC endpoints (no token needed) ───
                        .requestMatchers(HttpMethod.GET, 
                                "/api/events", 
                                "/api/events/").permitAll()
                        .requestMatchers(HttpMethod.GET, 
                                "/api/events/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/{id}/participants").permitAll()

                        // ─── ADMIN only endpoints ───
                        .requestMatchers(HttpMethod.POST, 
                                "/api/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, 
                                "/api/events/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, 
                                "/api/events/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/events/{id}/invite-top10").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/{id}/candidatures").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/events/candidature/{id}/accept").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/events/candidature/{id}/reject").hasRole("ADMIN")

                        // ─── AUTHENTICATED users (token required) ───
                        .requestMatchers(HttpMethod.POST,
                                "/api/events/{id}/register").authenticated()
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/events/{id}/register").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/me/registrations").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/me/invitations").authenticated()
                        .requestMatchers(HttpMethod.PUT,
                                "/api/events/{id}/invitation/accept").authenticated()
                        .requestMatchers(HttpMethod.PUT,
                                "/api/events/{id}/invitation/decline").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/api/events/{id}/candidature").authenticated()

                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
                .build();
    }

}
