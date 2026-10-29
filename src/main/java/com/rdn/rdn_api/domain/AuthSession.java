package com.rdn.rdn_api.domain;

import java.time.LocalDateTime;

/**
 * Sessao de autenticacao de um usuario.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao, getters/setters e
 * verificacao trivial de expiracao. Nao contem anotacoes de persistencia nem
 * regras de negocio (sem assinatura ou criptografia de token).</p>
 */
public class AuthSession {

    private User user;

    private String token;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    public AuthSession() {
    }

    /**
     * Verifica se o token esta preenchido e ainda nao expirou.
     *
     * @return {@code true} quando existe token e {@code expiresAt} e nulo ou
     *         posterior ao instante atual
     */
    public Boolean validateToken() {
        if (token == null || token.isBlank()) {
            return Boolean.FALSE;
        }

        return expiresAt == null || LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Revoga a sessao descartando o token.
     */
    public void revoke() {
        this.token = null;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

}
