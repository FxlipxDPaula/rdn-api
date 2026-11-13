package com.rdn.rdn_api.api.payment;

import com.rdn.rdn_api.application.payment.SimulationMode;
import com.rdn.rdn_api.domain.PaymentMethod;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /pedidos/{id}/pagamentos}.
 *
 * @param formaPagamento forma de pagamento
 * @param simular        desfecho desejado no provedor simulado (opcional; padrao APROVADO)
 */
public record RequestPaymentRequest(
        @NotNull(message = "formaPagamento e obrigatoria") PaymentMethod formaPagamento,
        SimulationMode simular) {
}
