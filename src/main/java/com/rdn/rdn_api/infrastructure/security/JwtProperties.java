package com.rdn.rdn_api.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do token JWT.
 *
 * <p>O segredo vem de variavel de ambiente ({@code JWT_SECRET}) e precisa ter
 * pelo menos 32 bytes para HS256; o valor padrao em
 * {@code application.properties} serve apenas para desenvolvimento local.</p>
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;

    private long expirationSeconds = 3600;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public void setExpirationSeconds(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }
}
