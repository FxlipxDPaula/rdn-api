package com.rdn.rdn_api.application.loyalty.exception;

/**
 * Levantada quando o cliente tenta usar fidelidade sem consentimento registrado.
 */
public class ConsentRequiredException extends RuntimeException {

    public ConsentRequiredException() {
        super("Consentimento de fidelidade nao registrado.");
    }
}
