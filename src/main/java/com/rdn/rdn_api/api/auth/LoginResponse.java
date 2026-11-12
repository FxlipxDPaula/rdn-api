package com.rdn.rdn_api.api.auth;

import java.util.UUID;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.auth.LoginResult;

/**
 * Corpo de sucesso de {@code POST /auth/login} e de {@code GET /auth/me}.
 *
 * <p>Nao inclui {@code passwordHash} nem qualquer dado sensivel: o usuario
 * trafega sempre como {@link UserSummary}.</p>
 *
 * @param accessToken token JWT
 * @param tokenType   esquema do token ({@code Bearer})
 * @param expiresIn   validade em segundos
 * @param user        dados publicos do usuario
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {

    /**
     * Cria a resposta a partir do resultado do caso de uso.
     *
     * @param result resultado do login
     * @return corpo HTTP de sucesso
     */
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.accessToken(), result.tokenType(), result.expiresInSeconds(),
                UserSummary.from(result.user()));
    }

    /**
     * Recorte publico do usuario autenticado.
     *
     * @param id       identificador
     * @param fullName nome completo
     * @param email    e-mail
     * @param role     perfil de acesso
     */
    public record UserSummary(UUID id, String fullName, String email, String role) {

        /**
         * Cria o recorte a partir da identidade autenticada.
         *
         * @param user identidade autenticada
         * @return dados publicos do usuario
         */
        public static UserSummary from(AuthenticatedUser user) {
            return new UserSummary(user.id(), user.fullName(), user.email(), user.role().name());
        }
    }
}
