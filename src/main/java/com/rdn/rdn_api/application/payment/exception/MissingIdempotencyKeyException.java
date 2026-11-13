package com.rdn.rdn_api.application.payment.exception;

/**
 * Requisicao de cobranca sem a chave de idempotencia.
 *
 * <p>Traduzida para {@code 400 CABECALHO_OBRIGATORIO}.</p>
 */
public class MissingIdempotencyKeyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MissingIdempotencyKeyException() {
        super("O cabecalho Idempotency-Key e obrigatorio para solicitar pagamento.");
    }
}
