package com.rdn.rdn_api.application.auth;

import com.rdn.rdn_api.domain.User;

/**
 * Porta de emissao de token de acesso.
 *
 * <p>O formato do token (JWT, assinatura, claims) e detalhe de infraestrutura;
 * a aplicacao so conhece o contrato de emissao.</p>
 */
public interface TokenIssuer {

    /**
     * Emite um token de acesso para o usuario.
     *
     * @param user usuario autenticado
     * @return token emitido e sua validade
     */
    IssuedToken issue(User user);

    /**
     * Token emitido.
     *
     * @param accessToken      valor do token assinado
     * @param expiresInSeconds validade em segundos
     */
    record IssuedToken(String accessToken, long expiresInSeconds) {
    }
}
