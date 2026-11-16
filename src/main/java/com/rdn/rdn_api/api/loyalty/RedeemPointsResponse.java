package com.rdn.rdn_api.api.loyalty;

import java.math.BigDecimal;

import com.rdn.rdn_api.application.loyalty.RedeemPointsResult;

/**
 * Resultado do resgate de pontos.
 *
 * @param pontosResgatados quantidade debitada
 * @param descontoAplicado valor abatido do pedido
 * @param novoSaldo        saldo restante
 * @param totalPedido      total recalculado
 */
public record RedeemPointsResponse(int pontosResgatados, BigDecimal descontoAplicado, int novoSaldo,
        BigDecimal totalPedido) {

    public static RedeemPointsResponse from(RedeemPointsResult result) {
        return new RedeemPointsResponse(result.pontosResgatados(), result.descontoAplicado(),
                result.novoSaldo(), result.totalPedido());
    }
}
