package com.rdn.rdn_api.api.payment;

import com.rdn.rdn_api.application.payment.SimulationMode;
import com.rdn.rdn_api.domain.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /pedidos/{id}/pagamentos}.
 *
 * @param formaPagamento forma de pagamento
 * @param simular        desfecho desejado no provedor simulado (opcional; padrao APROVADO)
 */
public record RequestPaymentRequest(
        @NotNull(message = "formaPagamento e obrigatoria")
        @Schema(description = "Forma de pagamento da cobranca", example = "MOCK")
        PaymentMethod formaPagamento,

        @Schema(description = "Desfecho desejado no provedor simulado; padrao APROVADO",
                example = "APROVADO", allowableValues = {"APROVADO", "RECUSADO", "FALHA"})
        SimulationMode simular) {
}
