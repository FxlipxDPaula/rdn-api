package com.rdn.rdn_api.application.order;

import com.rdn.rdn_api.domain.OrderStatus;

/**
 * Comando de mudanca de status do pedido.
 *
 * @param status status desejado
 * @param motivo motivo do cancelamento, usado quando {@code status} e
 *               {@link OrderStatus#CANCELLED}
 */
public record ChangeStatusCommand(OrderStatus status, String motivo) {
}
