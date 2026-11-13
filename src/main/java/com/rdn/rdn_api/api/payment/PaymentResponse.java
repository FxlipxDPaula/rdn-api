package com.rdn.rdn_api.api.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.Payment;
import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.PaymentStatus;

/**
 * Tentativa de pagamento na resposta HTTP.
 *
 * @param pagamentoId       identificador da tentativa
 * @param pedidoId          pedido cobrado
 * @param status            status do pagamento
 * @param valor             valor cobrado (igual ao total do pedido)
 * @param formaPagamento    forma de pagamento utilizada
 * @param transacaoGateway  identificador da transacao no provedor, quando aprovado
 * @param pedidoStatus      status do pedido depois do processamento
 * @param solicitadoEm      instante da solicitacao
 * @param processadoEm      instante da resposta do provedor
 */
public record PaymentResponse(UUID pagamentoId, UUID pedidoId, PaymentStatus status, BigDecimal valor,
        PaymentMethod formaPagamento, String transacaoGateway, OrderStatus pedidoStatus,
        LocalDateTime solicitadoEm, LocalDateTime processadoEm) {

    public static PaymentResponse from(Payment payment) {
        Order order = payment.getOrder();

        return new PaymentResponse(payment.getId(),
                order == null ? null : order.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getGatewayTransactionId(),
                order == null ? null : order.getStatus(),
                payment.getRequestedAt(),
                payment.getProcessedAt());
    }
}
