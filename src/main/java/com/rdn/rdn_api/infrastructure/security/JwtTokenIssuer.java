package com.rdn.rdn_api.infrastructure.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.rdn.rdn_api.application.auth.TokenIssuer;
import com.rdn.rdn_api.domain.User;

/**
 * Implementacao de {@link TokenIssuer} com JWT assinado em HMAC-SHA256.
 *
 * <p>Claims emitidas: {@code sub} (id do usuario), {@code email},
 * {@code name}, {@code roles} e as datas {@code iat}/{@code exp}. Nenhum dado
 * sensivel (como o hash da senha) entra no token, que e legivel por qualquer
 * portador.</p>
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private static final String ISSUER = "rdn-api";

    private final JwtEncoder jwtEncoder;

    private final JwtProperties jwtProperties;

    public JwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public IssuedToken issue(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.getExpirationSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        // O header precisa declarar HS256: sem ele o encoder tenta RS256 e nao
        // encontra a chave simetrica (erro "Failed to select a JWK signing key").
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(token, jwtProperties.getExpirationSeconds());
    }
}
