package com.rdn.rdn_api.application.auth;

/**
 * Usuario com credenciais corretas, porem desativado.
 *
 * <p>Separada de {@link InvalidCredentialsException} porque a resposta correta
 * e {@code 403}, e nao {@code 401}: a identidade existe, mas o acesso esta
 * negado por regra de negocio.</p>
 */
public class InactiveUserException extends RuntimeException {

    public InactiveUserException(String message) {
        super(message);
    }
}
