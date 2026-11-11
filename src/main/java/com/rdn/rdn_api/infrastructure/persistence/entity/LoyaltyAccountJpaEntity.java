package com.rdn.rdn_api.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Mapeamento relacional de {@code LoyaltyAccount}.
 *
 * <p>O consentimento e registrado na propria conta com data e hora, atendendo ao
 * requisito de base legal e rastreabilidade da LGPD.</p>
 */
@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private UserJpaEntity customer;

    @Column(name = "points_balance", nullable = false)
    private Integer pointsBalance;

    @Column(name = "consent_given", nullable = false)
    private Boolean consentGiven;

    @Column(name = "consent_timestamp")
    private LocalDateTime consentTimestamp;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserJpaEntity getCustomer() {
        return customer;
    }

    public void setCustomer(UserJpaEntity customer) {
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
