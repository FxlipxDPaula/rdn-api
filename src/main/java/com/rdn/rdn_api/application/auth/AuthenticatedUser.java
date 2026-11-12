package com.rdn.rdn_api.application.auth;

import java.util.UUID;

import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Identidade autenticada exposta pela camada de aplicacao.
 *
 * <p>Deliberadamente nao carrega {@code passwordHash}: e o recorte seguro do
 * {@link User} para trafegar em resposta HTTP e no security context (LGPD e
 * minimizacao de dados).</p>
 *
 * @param id       identificador do usuario
 * @param fullName nome completo
 * @param email    e-mail
 * @param role     perfil de acesso
 */
public record AuthenticatedUser(UUID id, String fullName, String email, UserRole role) {

    /**
     * Cria a identidade autenticada a partir do usuario de dominio.
     *
     * @param user usuario de dominio, obrigatorio
     * @return projecao segura do usuario
     */
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
