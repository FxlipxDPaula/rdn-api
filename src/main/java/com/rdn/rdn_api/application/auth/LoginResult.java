package com.rdn.rdn_api.application.auth;

/**
 * Saida do caso de uso de login.
 *
 * @param accessToken    token JWT assinado
 * @param tokenType      esquema do token, sempre {@code Bearer}
 * @param expiresInSeconds validade do token em segundos
 * @param user           identidade autenticada (sem dados sensiveis)
 */
public record LoginResult(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthenticatedUser user) {
}
