package com.rdn.rdn_api.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Conta de fidelidade de um cliente.
 *
 * <p>POJO de dominio: campos, construtor padrao, getters/setters e operacoes
 * triviais de consentimento e saldo de pontos, sempre null-safe. Nao contem
 * anotacoes de persistencia nem validacoes com excecoes.</p>
 */
public class LoyaltyAccount {

    private UUID id;

    private User customer;

    private Integer pointsBalance = 0;

    private Boolean consentGiven;

    private LocalDateTime consentTimestamp;

    public LoyaltyAccount() {
    }

    /**
     * Informa se o cliente consentiu com o tratamento de dados para fidelidade.
     *
     * @return {@code true} quando o consentimento esta ativo
     */
    public boolean hasConsent() {
        return Boolean.TRUE.equals(consentGiven);
    }

    /**
     * Registra o consentimento do cliente e o respectivo instante.
     */
    public void optIn() {
        this.consentGiven = Boolean.TRUE;
        this.consentTimestamp = LocalDateTime.now();
    }

    /**
     * Revoga o consentimento e limpa o instante de registro.
     */
    public void optOut() {
        this.consentGiven = Boolean.FALSE;
        this.consentTimestamp = null;
    }

    /**
     * Acrescenta pontos ao saldo.
     *
     * @param points quantidade de pontos a adicionar
     */
    public void addPoints(Integer points) {
        if (points == null || points <= 0) {
            return;
        }

        this.pointsBalance = (pointsBalance == null ? 0 : pointsBalance) + points;
    }

    /**
     * Resgata pontos do saldo, quando houver quantidade suficiente.
     *
     * @param points quantidade de pontos a resgatar
     * @return {@code true} quando o resgate foi efetuado, {@code false} quando a
     *         quantidade e nula, nao positiva ou superior ao saldo
     */
    public Boolean redeemPoints(Integer points) {
        if (points == null || points <= 0) {
            return Boolean.FALSE;
        }

        int balance = pointsBalance == null ? 0 : pointsBalance;
        if (balance < points) {
            return Boolean.FALSE;
        }

        this.pointsBalance = balance - points;
        return Boolean.TRUE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public LocalDateTime getConsentTimestamp() {
        return consentTimestamp;
    }

    public void setConsentTimestamp(LocalDateTime consentTimestamp) {
        this.consentTimestamp = consentTimestamp;
    }

}
