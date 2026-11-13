package com.rdn.rdn_api.application.payment;

import com.rdn.rdn_api.domain.PaymentMethod;

/**
 * Comando de solicitacao de pagamento.
 *
 * @param formaPagamento forma de pagamento aceita pelo provedor
 * @param simular        desfecho desejado no mock (opcional; padrao APROVADO)
 */
public record PaymentCommand(PaymentMethod formaPagamento, SimulationMode simular) {
}
