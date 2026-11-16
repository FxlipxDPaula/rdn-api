package com.rdn.rdn_api.api.loyalty;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /fidelidade/resgates}.
 *
 * @param pedidoId pedido que recebera o desconto
 * @param pontos   pontos a resgatar
 */
public record RedeemPointsRequest(@NotNull(message = "pedidoId e obrigatorio") UUID pedidoId,
        @Min(value = 1, message = "pontos deve ser maior que zero") int pontos) {
}
