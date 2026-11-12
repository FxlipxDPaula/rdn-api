package com.rdn.rdn_api.application.auth;

/**
 * Credenciais invalidas no login: e-mail inexistente ou senha incorreta.
 *
 * <p>Os dois casos compartilham a mesma excecao e a mesma mensagem de
 * proposito: distinguir "e-mail nao cadastrado" de "senha errada" permitiria
 * enumerar usuarios (vazamento de informacao).</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
