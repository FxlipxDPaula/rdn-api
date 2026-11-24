package com.rdn.rdn_api.api.order;

import com.rdn.rdn_api.domain.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code PATCH /pedidos/{id}/status}.
 *
 * @param status status desejado
 * @param motivo motivo do cancelamento, usado quando o status e {@code CANCELLED}
 */
public record ChangeOrderStatusRequest(
        @NotNull(message = "status e obrigatorio")
        @Schema(description = "Status desejado; a transicao e validada pelo dominio", example = "PREPARING")
        OrderStatus status,

        @Size(max = 255, message = "motivo deve ter no maximo 255 caracteres")
        @Schema(description = "Motivo do cancelamento, obrigatorio quando o status e CANCELLED",
                example = "cliente desistiu")
        String motivo) {
}
