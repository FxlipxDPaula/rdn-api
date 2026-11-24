package com.rdn.rdn_api.api.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.Payment;
import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.PaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

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
public record PaymentResponse(
        @Schema(description = "Identificador da tentativa de pagamento",
                example = "8f14e45f-ceea-467f-a8b5-1f2d3c4b5a69")
        UUID pagamentoId,

        @Schema(description = "Pedido cobrado", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID pedidoId,

        @Schema(description = "Status do pagamento no provedor", example = "APPROVED")
        PaymentStatus status,

        @Schema(description = "Valor cobrado, igual ao total do pedido", example = "46.62")
        BigDecimal valor,

        @Schema(description = "Forma de pagamento utilizada", example = "MOCK")
        PaymentMethod formaPagamento,

        @Schema(description = "Identificador da transacao no provedor, quando aprovado", example = "MOCK-9f8c1d2e")
        String transacaoGateway,

        @Schema(description = "Status do pedido depois do processamento", example = "RECEIVED")
        OrderStatus pedidoStatus,

        @Schema(description = "Instante da solicitacao", example = "2026-11-16T12:00:00")
        LocalDateTime solicitadoEm,

        @Schema(description = "Instante da resposta do provedor", example = "2026-11-16T12:00:01")
        LocalDateTime processadoEm) {

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
