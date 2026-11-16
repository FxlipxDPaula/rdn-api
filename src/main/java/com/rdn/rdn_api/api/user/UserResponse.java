package com.rdn.rdn_api.api.user;

import java.util.UUID;

import com.rdn.rdn_api.domain.User;
import com.rdn.rdn_api.domain.UserRole;

/**
 * Resposta de {@code POST /usuarios}.
 *
 * <p>Nao inclui {@code passwordHash}: o hash nunca sai do servidor.</p>
 *
 * @param usuarioId identificador gerado
 * @param nome      nome completo
 * @param email     e-mail normalizado
 * @param perfil    perfil de acesso
 * @param ativo     indicador de usuario ativo
 */
public record UserResponse(UUID usuarioId, String nome, String email, UserRole perfil, boolean ativo) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                Boolean.TRUE.equals(user.getIsActive()));
    }
}
