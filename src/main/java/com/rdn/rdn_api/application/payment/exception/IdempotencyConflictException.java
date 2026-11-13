package com.rdn.rdn_api.application.payment.exception;

/**
 * Chave de idempotencia reaproveitada em um pedido diferente.
 *
 * <p>Traduzida para {@code 409 CHAVE_IDEMPOTENCIA_CONFLITANTE}.</p>
 */
public class IdempotencyConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
