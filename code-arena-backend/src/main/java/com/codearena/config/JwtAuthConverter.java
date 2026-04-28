package com.codearena.config;

import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        Collection<GrantedAuthority> authorities = extractAuthorities(source);
        return new JwtAuthenticationToken(source, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null) {
            return Collections.emptySet();
        }

        try {
            return userRepository.findByAuth0Id(sub)
                    .map(User::getRole)
                    .map(role -> (Collection<GrantedAuthority>) java.util.Set.<GrantedAuthority>of(
                            new SimpleGrantedAuthority("ROLE_" + role.name())
                    ))
                    .orElse(Collections.emptySet());
        } catch (DataAccessException ex) {
            log.debug("Skipping JWT role lookup for {} because the database is unavailable: {}", sub, ex.getMostSpecificCause().getMessage());
            return Collections.emptySet();
        }
    }
}
