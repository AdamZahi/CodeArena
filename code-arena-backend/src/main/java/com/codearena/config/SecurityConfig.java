package com.codearena.config;

import com.codearena.user.security.UserSyncFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String ROLES_CLAIM = "https://codearena.com/roles";

    /**
     * Configures JWT decoder with Auth0 audience validation.
     *
     * @param auth0Config Auth0 configuration
     * @return configured JWT decoder
     */
    @Bean
    public JwtDecoder jwtDecoder(Auth0Config auth0Config) {
        String issuer = String.format("https://%s/", auth0Config.getDomain());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new AudienceValidator(auth0Config.getAudience())
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }

    /**
     * Configures security filter chain.
     *
     * @param http HttpSecurity
     * @return configured filter chain
     * @throws Exception when configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserSyncFilter userSyncFilter) throws Exception {
        // TODO: Expand role matrix for additional modules.
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/*").hasAnyAuthority("ROLE_ADMIN", "ROLE_COACH")
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class)
            .build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }
}
