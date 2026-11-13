package com.rdn.rdn_api.application.payment;

/**
 * Resposta do provedor de pagamento.
 *
 * @param approved        {@code true} quando a cobranca foi aprovada
 * @param failed          {@code true} quando houve falha tecnica (timeout, indisponibilidade)
 * @param transactionId   identificador da transacao no provedor, quando houver
 * @param rawResponse     payload bruto, guardado para auditoria
 */
public record ChargeResult(boolean approved, boolean failed, String transactionId, String rawResponse) {
}
