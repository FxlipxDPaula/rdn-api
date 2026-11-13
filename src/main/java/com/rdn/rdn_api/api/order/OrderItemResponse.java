package com.rdn.rdn_api.api.order;

import java.math.BigDecimal;
import java.util.UUID;

import com.rdn.rdn_api.domain.OrderItem;

/**
 * Item do pedido na resposta HTTP.
 *
 * @param produtoId     item de cardapio
 * @param produtoNome   nome do produto no momento da resposta
 * @param quantidade    quantidade pedida
 * @param precoUnitario preco praticado no momento do pedido
 * @param subtotal      preco unitario multiplicado pela quantidade
 * @param observacao    texto livre registrado no item
 */
public record OrderItemResponse(UUID produtoId, String produtoNome, Integer quantidade,
        BigDecimal precoUnitario, BigDecimal subtotal, String observacao) {

    public static OrderItemResponse from(OrderItem item) {
        UUID produtoId = item.getMenuItem() == null ? null : item.getMenuItem().getId();
        String produtoNome = item.getMenuItem() == null ? null : item.getMenuItem().getName();

        return new OrderItemResponse(produtoId, produtoNome, item.getQuantity(),
                item.getUnitPrice(), item.getSubtotal(), item.getNotes());
    }
}
