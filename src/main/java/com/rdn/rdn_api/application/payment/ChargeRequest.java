package com.rdn.rdn_api.application.payment;

import java.math.BigDecimal;
import java.util.UUID;

import com.rdn.rdn_api.domain.PaymentMethod;

/**
 * Pedido de cobranca enviado ao provedor de pagamento.
 *
 * @param orderId        pedido cobrado
 * @param amount         valor calculado pelo dominio (nunca informado pelo cliente)
 * @param method         forma de pagamento
 * @param idempotencyKey chave que identifica a tentativa perante o provedor
 * @param simular        desfecho desejado no provedor simulado
 */
public record ChargeRequest(UUID orderId, BigDecimal amount, PaymentMethod method,
        String idempotencyKey, SimulationMode simular) {
}
