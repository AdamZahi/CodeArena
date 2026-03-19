package com.codearena.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Converts JWT claims into authentication token.
     *
     * @param source JWT source
     * @return authentication token
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        // TODO: Extract realm_access roles and map to authorities.
        return null;
    }
}
