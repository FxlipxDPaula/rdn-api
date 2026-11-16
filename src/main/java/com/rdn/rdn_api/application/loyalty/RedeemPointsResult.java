package com.rdn.rdn_api.application.loyalty;

import java.math.BigDecimal;

/**
 * Resultado do resgate de pontos.
 *
 * @param pontosResgatados quantidade debitada
 * @param descontoAplicado valor abatido do pedido
 * @param novoSaldo        saldo restante
 * @param totalPedido      total recalculado do pedido
 */
public record RedeemPointsResult(int pontosResgatados, BigDecimal descontoAplicado, int novoSaldo,
        BigDecimal totalPedido) {
}
