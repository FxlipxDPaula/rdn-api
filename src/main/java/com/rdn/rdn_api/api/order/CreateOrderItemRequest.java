package com.rdn.rdn_api.api.order;

import java.util.UUID;

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
        @NotNull(message = "produtoId e obrigatorio") UUID produtoId,
        @NotNull(message = "quantidade e obrigatoria") @Min(value = 1, message = "quantidade deve ser maior que zero") Integer quantidade,
        @Size(max = 255, message = "observacao deve ter no maximo 255 caracteres") String observacao) {
}
