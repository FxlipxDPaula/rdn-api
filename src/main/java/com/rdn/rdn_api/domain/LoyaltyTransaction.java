package com.rdn.rdn_api.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lancamento de pontos em uma conta de fidelidade.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao e getters/setters. Nao
 * contem anotacoes de persistencia nem regras de negocio.</p>
 */
public class LoyaltyTransaction {

    private UUID id;

    private LoyaltyAccount loyaltyAccount;

    private Order order;

    private Integer points;

    private TransactionType type;

    private LocalDateTime createdAt;

    public LoyaltyTransaction() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LoyaltyAccount getLoyaltyAccount() {
        return loyaltyAccount;
    }

    public void setLoyaltyAccount(LoyaltyAccount loyaltyAccount) {
        this.loyaltyAccount = loyaltyAccount;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
