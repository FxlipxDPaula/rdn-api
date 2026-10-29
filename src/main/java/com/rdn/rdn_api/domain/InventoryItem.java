package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Estoque de um item de cardapio em uma filial.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao, getters/setters e
 * operacoes triviais de soma e subtracao de saldo. Nao contem anotacoes de
 * persistencia nem regras de negocio (sem validacoes e sem excecoes de
 * dominio). O parametro {@code reason} e aceito para uso futuro, mas nao e
 * armazenado aqui: o registro do motivo pertence a {@link InventoryMovement}.</p>
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
     */
    public void addStock(BigDecimal quantity, String reason) {
        if (quantity == null) {
            return;
        }
        quantityOnHand = (quantityOnHand == null ? BigDecimal.ZERO : quantityOnHand).add(quantity);
    }

    /**
     * Deduz quantidade do saldo em estoque.
     *
     * @param quantity quantidade a ser retirada
     * @param reason   motivo da saida
     */
    public void deductStock(BigDecimal quantity, String reason) {
        if (quantity == null) {
            return;
        }
        quantityOnHand = (quantityOnHand == null ? BigDecimal.ZERO : quantityOnHand).subtract(quantity);
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
