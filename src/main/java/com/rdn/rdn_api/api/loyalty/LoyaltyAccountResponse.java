package com.rdn.rdn_api.api.loyalty;

import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.application.loyalty.LoyaltyAccountView;

/**
 * Conta de fidelidade na resposta HTTP.
 *
 * @param clienteId       identificador do cliente
 * @param saldoPontos     saldo atual
 * @param consentimento   indicador de opt-in
 * @param consentimentoEm instante do ultimo opt-in
 */
public record LoyaltyAccountResponse(UUID clienteId, int saldoPontos, boolean consentimento,
        LocalDateTime consentimentoEm) {

    public static LoyaltyAccountResponse from(LoyaltyAccountView view) {
        return new LoyaltyAccountResponse(view.clienteId(), view.saldoPontos(), view.consentimento(),
                view.consentimentoEm());
    }
}
