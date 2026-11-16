package com.rdn.rdn_api.api.loyalty;

import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.domain.LoyaltyTransaction;
import com.rdn.rdn_api.domain.TransactionType;

/**
 * Lancamento do extrato de fidelidade.
 *
 * @param transacaoId identificador do lancamento
 * @param pontos      pontos assinados (positivo para EARNED, negativo para REDEEMED)
 * @param tipo        natureza do lancamento
 * @param pedidoId    pedido relacionado, quando houver
 * @param criadoEm    instante do lancamento
 */
public record LoyaltyTransactionResponse(UUID transacaoId, Integer pontos, TransactionType tipo,
        UUID pedidoId, LocalDateTime criadoEm) {

    public static LoyaltyTransactionResponse from(LoyaltyTransaction transaction) {
        UUID orderId = transaction.getOrder() == null ? null : transaction.getOrder().getId();

        return new LoyaltyTransactionResponse(transaction.getId(), transaction.getPoints(),
                transaction.getType(), orderId, transaction.getCreatedAt());
    }
}
