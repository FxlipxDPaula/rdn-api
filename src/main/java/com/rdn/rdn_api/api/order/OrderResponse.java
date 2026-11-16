package com.rdn.rdn_api.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;

/**
 * Pedido completo na resposta HTTP.
 *
 * @param pedidoId            identificador do pedido
 * @param canalPedido         canal de origem
 * @param status              status atual
 * @param unidadeId           unidade que produz o pedido
 * @param clienteId           cliente vinculado, quando houver
 * @param total               total calculado pelo dominio
 * @param desconto            desconto acumulado (promocao e/ou resgate)
 * @param promocaoId          campanha aplicada, quando houver
 * @param itens               itens do pedido
 * @param motivoCancelamento  motivo registrado quando cancelado
 * @param criadoEm            instante de criacao
 */
public record OrderResponse(UUID pedidoId, SalesChannel canalPedido, OrderStatus status, UUID unidadeId,
        UUID clienteId, BigDecimal total, BigDecimal desconto, UUID promocaoId,
        List<OrderItemResponse> itens, String motivoCancelamento, LocalDateTime criadoEm) {

    public static OrderResponse from(Order order) {
        UUID unidadeId = order.getBranch() == null ? null : order.getBranch().getId();
        UUID clienteId = order.getCustomer() == null ? null : order.getCustomer().getId();
        UUID promocaoId = order.getPromotion() == null ? null : order.getPromotion().getId();

        List<OrderItemResponse> itens = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(OrderItemResponse::from).toList();

        return new OrderResponse(order.getId(), order.getOriginChannel(), order.getStatus(), unidadeId,
                clienteId, order.getTotalAmount(), order.getDiscountAmount(), promocaoId, itens,
                order.getCancellationReason(), order.getCreatedAt());
    }
}
