package com.rdn.rdn_api.application.loyalty;

import java.util.UUID;

/**
 * Comando de resgate de pontos em um pedido.
 *
 * @param pedidoId pedido que recebera o desconto
 * @param pontos   quantidade de pontos a debitar
 */
public record RedeemPointsCommand(UUID pedidoId, int pontos) {
}
