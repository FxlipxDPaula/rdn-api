package com.rdn.rdn_api.api.order;

import com.rdn.rdn_api.domain.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code PATCH /pedidos/{id}/status}.
 *
 * @param status status desejado
 * @param motivo motivo do cancelamento, usado quando o status e {@code CANCELLED}
 */
public record ChangeOrderStatusRequest(
        @NotNull(message = "status e obrigatorio") OrderStatus status,
        @Size(max = 255, message = "motivo deve ter no maximo 255 caracteres") String motivo) {
}
