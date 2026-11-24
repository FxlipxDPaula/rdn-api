package com.rdn.rdn_api.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.rdn.rdn_api.domain.Order;
import com.rdn.rdn_api.domain.OrderStatus;
import com.rdn.rdn_api.domain.SalesChannel;

import io.swagger.v3.oas.annotations.media.Schema;

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
public record OrderResponse(
        @Schema(description = "Identificador do pedido", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID pedidoId,

        @Schema(description = "Canal de origem do pedido", example = "TOTEM")
        SalesChannel canalPedido,

        @Schema(description = "Status atual do pedido", example = "AGUARDANDO_PAGAMENTO")
        OrderStatus status,

        @Schema(description = "Unidade que produz o pedido",
                example = "00000000-0000-0000-0000-000000000001")
        UUID unidadeId,

        @Schema(description = "Cliente vinculado ao pedido, quando houver",
                example = "00000000-0000-0000-0000-00000000000f")
        UUID clienteId,

        @Schema(description = "Total calculado pelo dominio", example = "46.62")
        BigDecimal total,

        @Schema(description = "Desconto acumulado de promocao e resgate", example = "5.18")
        BigDecimal desconto,

        @Schema(description = "Campanha aplicada ao pedido, quando houver",
                example = "00000000-0000-0000-0000-0000000000c1")
        UUID promocaoId,

        @Schema(description = "Itens do pedido com preco praticado")
        List<OrderItemResponse> itens,

        @Schema(description = "Motivo registrado quando o pedido foi cancelado")
        String motivoCancelamento,

        @Schema(description = "Instante de criacao do pedido", example = "2026-11-16T12:00:00")
        LocalDateTime criadoEm) {

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
