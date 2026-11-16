package com.rdn.rdn_api.application.loyalty;

import java.time.LocalDateTime;
import java.util.UUID;

import com.rdn.rdn_api.domain.LoyaltyAccount;

/**
 * Visao da conta de fidelidade para o cliente autenticado.
 *
 * @param clienteId       identificador do cliente
 * @param saldoPontos     saldo atual
 * @param consentimento   indicador de opt-in
 * @param consentimentoEm instante do ultimo opt-in
 */
public record LoyaltyAccountView(UUID clienteId, int saldoPontos, boolean consentimento,
        LocalDateTime consentimentoEm) {

    /**
     * Visao neutra para cliente que ainda nao possui conta.
     *
     * @param clienteId identificador do cliente
     * @return conta com saldo zero e sem consentimento
     */
    public static LoyaltyAccountView empty(UUID clienteId) {
        return new LoyaltyAccountView(clienteId, 0, false, null);
    }

    /**
     * Converte a conta de dominio.
     *
     * @param account conta, possivelmente nula
     * @return visao correspondente
     */
    public static LoyaltyAccountView from(LoyaltyAccount account) {
        if (account == null) {
            return empty(null);
        }

        UUID customerId = account.getCustomer() == null ? null : account.getCustomer().getId();
        int balance = account.getPointsBalance() == null ? 0 : account.getPointsBalance();

        return new LoyaltyAccountView(customerId, balance, account.hasConsent(),
                account.getConsentTimestamp());
    }
}
