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

    /**
     * Cria um lancamento de pontos ganhos.
     *
     * @param account   conta de fidelidade
     * @param order     pedido que originou os pontos
     * @param points    quantidade positiva de pontos
     * @param createdAt instante do lancamento
     * @return lancamento com {@link TransactionType#EARNED} e pontos positivos
     */
    public static LoyaltyTransaction earned(LoyaltyAccount account, Order order, int points,
            LocalDateTime createdAt) {
        return build(account, order, points, TransactionType.EARNED, createdAt);
    }

    /**
     * Cria um lancamento de pontos resgatados.
     *
     * @param account   conta de fidelidade
     * @param order     pedido em que o resgate foi aplicado
     * @param points    quantidade positiva de pontos debitados
     * @param createdAt instante do lancamento
     * @return lancamento com {@link TransactionType#REDEEMED} e pontos negativos
     */
    public static LoyaltyTransaction redeemed(LoyaltyAccount account, Order order, int points,
            LocalDateTime createdAt) {
        return build(account, order, points, TransactionType.REDEEMED, createdAt);
    }

    private static LoyaltyTransaction build(LoyaltyAccount account, Order order, int points,
            TransactionType type, LocalDateTime createdAt) {
        if (account == null) {
            throw new IllegalArgumentException("conta de fidelidade e obrigatoria");
        }
        if (points <= 0) {
            throw new IllegalArgumentException("pontos devem ser positivos");
        }

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setLoyaltyAccount(account);
        transaction.setOrder(order);
        transaction.setPoints(TransactionType.EARNED == type ? points : -points);
        transaction.setType(type);
        transaction.setCreatedAt(createdAt == null ? LocalDateTime.now() : createdAt);

        return transaction;
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
