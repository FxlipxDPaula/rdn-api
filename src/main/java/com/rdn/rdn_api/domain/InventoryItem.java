package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Estoque de um item de cardapio em uma filial.
 *
 * <p>POJO de dominio: campos, construtor padrao, getters/setters e as regras
 * que protegem o saldo. Nao contem anotacoes de persistencia e nao lanca
 * excecoes: operacoes de saldo recusadas retornam {@code false}. O parametro
 * {@code reason} e aceito para uso futuro, mas nao e armazenado aqui: o
 * registro do motivo pertence a {@link InventoryMovement}.</p>
 *
 * <p>Invariante: o saldo nunca fica negativo. Uma saida maior que o saldo
 * disponivel e recusada, o que permite a camada de aplicacao responder conflito
 * de regra de negocio (estoque insuficiente) sem depender do banco.</p>
 */
public class InventoryItem {

    private UUID id;

    private Branch branch;

    private UUID menuItemId;

    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    private BigDecimal minimumThreshold = BigDecimal.ZERO;

    public InventoryItem() {
    }

    /**
     * Acrescenta quantidade ao saldo em estoque.
     *
     * @param quantity quantidade a ser adicionada
     * @param reason   motivo da entrada
     * @return {@code true} quando o saldo foi atualizado, {@code false} quando a
     *         quantidade e nula ou nao positiva
     */
    public Boolean addStock(BigDecimal quantity, String reason) {
        if (quantity == null || quantity.signum() <= 0) {
            return Boolean.FALSE;
        }

        quantityOnHand = (quantityOnHand == null ? BigDecimal.ZERO : quantityOnHand).add(quantity);
        return Boolean.TRUE;
    }

    /**
     * Deduz quantidade do saldo em estoque.
     *
     * <p>A deducao e recusada quando a quantidade nao for positiva ou quando
     * exceder o saldo disponivel, garantindo o invariante de saldo nao
     * negativo.</p>
     *
     * @param quantity quantidade a ser retirada
     * @param reason   motivo da saida
     * @return {@code true} quando o saldo foi atualizado, {@code false} quando a
     *         quantidade e invalida ou superior ao saldo
     */
    public Boolean deductStock(BigDecimal quantity, String reason) {
        if (quantity == null || quantity.signum() <= 0) {
            return Boolean.FALSE;
        }

        BigDecimal current = quantityOnHand == null ? BigDecimal.ZERO : quantityOnHand;
        if (current.compareTo(quantity) < 0) {
            return Boolean.FALSE;
        }

        quantityOnHand = current.subtract(quantity);
        return Boolean.TRUE;
    }

    /**
     * Informa se ha saldo acima do limite minimo configurado.
     *
     * @return {@code true} quando o saldo e maior que o limite minimo
     */
    public Boolean isAvailable() {
        if (quantityOnHand == null || minimumThreshold == null) {
            return Boolean.FALSE;
        }

        return quantityOnHand.compareTo(minimumThreshold) > 0;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public UUID getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public BigDecimal getMinimumThreshold() {
        return minimumThreshold;
    }

    public void setMinimumThreshold(BigDecimal minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

}
