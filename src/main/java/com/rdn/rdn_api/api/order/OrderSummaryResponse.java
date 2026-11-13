package com.rdn.rdn_api.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;

/**
 * Resumo de pedido usado na listagem paginada.
 *
 * <p>Nao carrega itens de proposito: a listagem nao deve paginar colecao nem
 * disparar consultas por linha.</p>
 *
 * @param pedidoId    identificador do pedido
 * @param canalPedido canal de origem
 * @param status      status atual
 * @param total       total calculado
 * @param criadoEm    instante de criacao
 */
public record OrderSummaryResponse(UUID pedidoId, SalesChannel canalPedido, OrderStatus status,
        BigDecimal total, LocalDateTime criadoEm) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getOriginChannel(), order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt());
    }
}
