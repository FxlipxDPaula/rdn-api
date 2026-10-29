package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de um pedido.
 *
 * <p>POJO de dominio: apenas campos, construtores, getters/setters e o calculo
 * trivial de subtotal. Nao contem anotacoes de persistencia nem regras de
 * negocio (sem validacoes e sem excecoes de dominio).</p>
 */
public class OrderItem {

    private UUID id;

    private MenuItem menuItem;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal = BigDecimal.ZERO;

    private String notes;

    public OrderItem() {
    }

    /**
     * Cria um item de pedido com produto e quantidade.
     *
     * <p>O preco unitario nao e preenchido aqui; ele e copiado do
     * {@link MenuItem} por {@link Order#addItem(MenuItem, Integer)}, ou pode ser
     * definido por {@link #setUnitPrice(BigDecimal)}.</p>
     *
     * @param menuItem produto do cardapio
     * @param quantity quantidade desejada
     */
    public OrderItem(MenuItem menuItem, Integer quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    /**
     * Calcula o subtotal do item como preco unitario multiplicado pela
     * quantidade, atualizando o campo {@code subtotal}.
     *
     * @return subtotal calculado, ou {@link BigDecimal#ZERO} quando o preco
     *         unitario ou a quantidade forem nulos
     */
    public BigDecimal calculateSubtotal() {
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        int qty = quantity == null ? 0 : quantity;

        subtotal = price.multiply(BigDecimal.valueOf(qty));
        return subtotal;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}
