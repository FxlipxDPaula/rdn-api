package com.rdn.rdn_api.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item do cardapio de uma filial.
 *
 * <p>POJO de dominio: apenas campos, construtor padrao, getters/setters e
 * operacoes triviais. Nao contem anotacoes de persistencia nem regras de
 * negocio (sem validacoes e sem excecoes de dominio).</p>
 */
public class MenuItem {

    private UUID id;

    private String name;

    private String description;

    private BigDecimal price;

    private Boolean isAvailable;

    public MenuItem() {
    }

    /**
     * Atualiza o preco do item.
     *
     * @param newPrice novo preco
     */
    public void updatePrice(BigDecimal newPrice) {
        this.price = newPrice;
    }

    /**
     * Define a disponibilidade do item no cardapio.
     *
     * @param status {@code true} para disponivel, {@code false} para indisponivel
     */
    public void setAvailability(Boolean status) {
        this.isAvailable = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(isAvailable);
    }

}
