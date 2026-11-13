package com.rdn.rdn_api.application.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Porta de saida para o estoque por unidade.
 *
 * <p>A reserva e uma operacao unica de proposito: ler o saldo, aplicar a baixa
 * e registrar a movimentacao precisam acontecer na mesma transacao e sob lock,
 * senao duas comandas simultaneas venderiam o mesmo saldo.</p>
 */
public interface InventoryGateway {

    /**
     * Tenta baixar a quantidade do estoque da unidade e registrar a saida.
     *
     * @param branchId   unidade
     * @param menuItemId item de cardapio
     * @param quantity   quantidade solicitada
     * @param notes      observacao gravada na movimentacao
     * @return resultado com indicacao de sucesso e saldo disponivel antes da baixa
     */
    ReservationResult reserve(UUID branchId, UUID menuItemId, BigDecimal quantity, String notes);

    /**
     * Resultado de uma tentativa de reserva.
     *
     * @param reserved  {@code true} quando a baixa foi aplicada
     * @param available saldo que existia antes da tentativa
     */
    record ReservationResult(boolean reserved, BigDecimal available) {
    }
}
