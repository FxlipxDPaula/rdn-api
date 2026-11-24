package com.rdn.rdn_api.api.order;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Item do pedido no corpo da requisicao.
 *
 * @param produtoId  item de cardapio
 * @param quantidade quantidade desejada (minimo 1)
 * @param observacao texto livre para a cozinha
 */
public record CreateOrderItemRequest(
        @NotNull(message = "produtoId e obrigatorio")
        @Schema(description = "Item de cardapio pedido",
                example = "00000000-0000-0000-0000-000000000003")
        UUID produtoId,

        @NotNull(message = "quantidade e obrigatoria")
        @Min(value = 1, message = "quantidade deve ser maior que zero")
        @Schema(description = "Quantidade desejada (minimo 1)", example = "2")
        Integer quantidade,

        @Size(max = 255, message = "observacao deve ter no maximo 255 caracteres")
        @Schema(description = "Texto livre para a cozinha", example = "sem cebola")
        String observacao) {
}
