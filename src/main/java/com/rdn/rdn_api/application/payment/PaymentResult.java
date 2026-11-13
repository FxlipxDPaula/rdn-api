package com.rdn.rdn_api.application.payment;

import com.rdn.rdn_api.domain.Payment;

/**
 * Resultado de uma solicitacao de pagamento.
 *
 * @param payment tentativa processada (ou reaproveitada)
 * @param replay  {@code true} quando a requisicao repetiu uma chave de idempotencia
 *                ja processada
 */
public record PaymentResult(Payment payment, boolean replay) {
}
