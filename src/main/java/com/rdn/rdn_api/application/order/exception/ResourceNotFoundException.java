package com.rdn.rdn_api.application.order.exception;

/**
 * Recurso referenciado por uma operacao nao existe.
 *
 * <p>Traduzida para {@code 404 RECURSO_NAO_ENCONTRADO} pela borda HTTP.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String recurso, Object identificador) {
        super(recurso + " nao encontrado(a): " + identificador);
    }

    public ResourceNotFoundException(String mensagem) {
        super(mensagem);
    }
}
