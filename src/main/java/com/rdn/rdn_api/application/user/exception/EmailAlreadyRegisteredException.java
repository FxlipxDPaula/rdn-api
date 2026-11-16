package com.rdn.rdn_api.application.user.exception;

/**
 * Lancada quando o e-mail informado ja pertence a outro usuario.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
